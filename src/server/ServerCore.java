package server;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ServerCore {

    public static final String SERVER_DIR = "F:/MailServerJavaFX/server_mail";
    public static final int    PORT       = 2023;

    public interface Logger {
        void log(String msg);
    }

    private final Logger logger;
    private volatile boolean running = true;

    public static class ClientInfo {
        public String username;
        public String ip;
        public long   lastSeen;
        ClientInfo(String u, String i, long t) { username = u; ip = i; lastSeen = t; }
    }
    private final Map<String, ClientInfo> onlineClients = new LinkedHashMap<>();

    public ServerCore(Logger logger) {
        this.logger = logger;
        new File(SERVER_DIR).mkdirs();
    }

    public Map<String, ClientInfo> getOnlineClients() {
        long now = System.currentTimeMillis();
        onlineClients.entrySet().removeIf(e -> (now - e.getValue().lastSeen) > 10000);
        return onlineClients;
    }

    public void start(int port) {
        new Thread(() -> {
            try {
                DatagramSocket socket = new DatagramSocket(port, InetAddress.getByName("0.0.0.0"));
                logger.log("[OK] Server started on port " + port);
                logger.log("[DIR] " + new File(SERVER_DIR).getAbsolutePath());

                byte[] recvBuf = new byte[16384];
                while (running) {
                    DatagramPacket recvPkt = new DatagramPacket(recvBuf, recvBuf.length);
                    socket.receive(recvPkt);

                    InetAddress ip = recvPkt.getAddress();
                    int portCli = recvPkt.getPort();

                    String req = new String(recvPkt.getData(), 0, recvPkt.getLength(), "UTF-8").trim();
                    logger.log("[RECV] " + ip.getHostAddress() + ":" + portCli + " | " + shorten(req));

                    if (req.startsWith("LOGIN|")) {
                        String[] parts = req.split("\\|", -1);
                        if (parts.length >= 2) {
                            onlineClients.put(parts[1], new ClientInfo(parts[1], ip.getHostAddress(), System.currentTimeMillis()));
                        }
                    }

                    String resp = handle(req, ip);
                    byte[] sendBuf = resp.getBytes("UTF-8");
                    socket.send(new DatagramPacket(sendBuf, sendBuf.length, ip, portCli));
                }
            } catch (Exception e) {
                logger.log("[ERR] Server error: " + e.getMessage());
            }
        }, "UDP-Server").start();
    }

    public void stop() { running = false; }

    private String shorten(String s) {
        if (s.length() > 120) return s.substring(0, 120) + "...";
        return s;
    }

    private String handle(String req, InetAddress clientIP) {
        String[] p = req.split("\\|", -1);
        if (p.length == 0 || p[0].isEmpty()) return "ERROR|Empty request";

        switch (p[0].toUpperCase()) {
            case "REGISTER":
                if (p.length < 3) return "ERROR|Missing username/password";
                return register(p[1], p[2]);
            case "LOGIN":
                if (p.length < 3) return "ERROR|Missing username/password";
                return login(p[1], p[2]);
            case "SEND":
                if (p.length < 6) return "ERROR|Missing fields";
                return sendMail(p[1], p[2], p[3], p[4], p[5], clientIP);
            case "LIST":
                return listUsers();
            case "GET":
                if (p.length < 3) return "ERROR|Missing params";
                return getMailContent(p[1], p[2]);
            case "DELETE":
                if (p.length < 3) return "ERROR|Missing params";
                return deleteMail(p[1], p[2]);
            case "PING":
                return "OK|PONG";
            default:
                return "ERROR|Unknown command";
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  YÊU CẦU #1: ĐĂNG KÝ
    //  Tạo folder: server_mail/<user>/
    //  CHỈ 1 FILE DUY NHẤT: new_email.txt (3 dòng User/Password/Created)
    //  KHÔNG có folder con
    // ═══════════════════════════════════════════════════════════
    private String register(String user, String pass) {
        try {
            File dir = new File(SERVER_DIR, user);
            if (dir.exists()) return "ERROR|Account already exists";
            dir.mkdirs();

            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            // ⭐ CHỈ 1 FILE: new_email.txt
            write(new File(dir, "new_email.txt"),
                  "User     : " + user + "\n" +
                  "Password : " + pass + "\n" +
                  "Created  : " + now  + "\n");

            logger.log("[OK] Registered: " + user);
            return "OK|Register successful. Your email: " + user + "@gmail.com";
        } catch (Exception e) {
            return "ERROR|" + e.getMessage();
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  YÊU CẦU #3: LOGIN
    //  Đọc User/Password từ new_email.txt
    //  Trả về danh sách TÊN FILE trong folder account
    // ═══════════════════════════════════════════════════════════
    private String login(String user, String pass) {
        try {
            File dir = new File(SERVER_DIR, user);
            if (!dir.exists()) return "ERROR|Account not found";

            File info = new File(dir, "new_email.txt");
            if (!info.exists()) return "ERROR|Corrupted account";

            String content = read(info);
            String sU = extract(content, "User");
            String sP = extract(content, "Password");

            if (!sU.equals(user) || !sP.equals(pass))
                return "ERROR|Wrong username or password";

            String myEmail = user + "@gmail.com";

            StringBuilder sb = new StringBuilder("OK|Login successful\n");
            sb.append("EMAIL|").append(myEmail).append("\n");
            sb.append("SECTION|INBOX\n");

            // ⭐ Trả về danh sách TÊN FILE trong folder account
            File[] files = dir.listFiles();
            if (files != null) {
                Arrays.sort(files, Comparator.comparing(File::getName));
                for (File f : files) {
                    if (f.isFile()) {
                        // Đọc thông tin mail để client hiển thị
                        String mailContent = read(f);
                        if (f.getName().startsWith("email_")) {
                            // Đây là mail → parse
                            String from    = extract(mailContent, "From");
                            String fromIp  = extract(mailContent, "FromIP");
                            String to      = extract(mailContent, "To");
                            String subj    = extract(mailContent, "Subject");
                            String time    = extract(mailContent, "SentTime");
                            String body    = extractBody(mailContent);

                            sb.append("MAIL|INBOX|")
                              .append(f.getName()).append("|")
                              .append(from).append("|")
                              .append(to).append("|")
                              .append(subj).append("|")
                              .append(time).append("|")
                              .append(body.replace("\n", " ")).append("|")
                              .append("0").append("\n");
                        } else {
                            // File hệ thống (new_email.txt)
                            sb.append("ACCOUNTFILE|").append(f.getName()).append("\n");
                        }
                    }
                }
            }

            return sb.toString();
        } catch (Exception e) {
            return "ERROR|" + e.getMessage();
        }
    }

    private String getMailContent(String user, String fileName) {
        try {
            File dir = new File(SERVER_DIR, user);
            File mail = new File(dir, fileName);
            if (!mail.exists()) return "ERROR|Mail not found";
            String content = read(mail);
            return "OK|" + content;
        } catch (Exception e) {
            return "ERROR|" + e.getMessage();
        }
    }

    private String deleteMail(String user, String fileName) {
        try {
            File dir = new File(SERVER_DIR, user);
            File mail = new File(dir, fileName);
            if (!mail.exists()) return "ERROR|Mail not found";
            mail.delete();
            return "OK|Deleted";
        } catch (Exception e) {
            return "ERROR|" + e.getMessage();
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  YÊU CẦU #2: GỬI MAIL
    //  Tạo 1 FILE trong thư mục NGƯỜI NHẬN với 4 thông tin:
    //    FromIP / SentTime / Subject / Body
    // ═══════════════════════════════════════════════════════════
    private String sendMail(String sender, String fromEmail, String toEmail,
                            String subject, String content,
                            InetAddress senderIP) {
        try {
            String receiver = toEmail.contains("@")
                    ? toEmail.substring(0, toEmail.indexOf('@'))
                    : toEmail;

            File rDir = new File(SERVER_DIR, receiver);
            if (!rDir.exists()) return "ERROR|Receiver does not exist";

            // ⭐ Tạo file mail trực tiếp trong folder người nhận
            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
            String fname = "email_" + sender + "_" + ts + ".txt";

            String sentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            StringBuilder body = new StringBuilder();
            body.append("From: ").append(sender).append("\n");
            body.append("FromIP: ").append(senderIP.getHostAddress()).append("\n");
            body.append("To: ").append(receiver).append("\n");
            body.append("Subject: ").append(subject).append("\n");
            body.append("SentTime: ").append(sentTime).append("\n");
            body.append("\n");
            body.append(content).append("\n");

            // ⭐ Lưu TRỰC TIẾP vào folder người nhận (không cần folder con inbox)
            write(new File(rDir, fname), body.toString());

            logger.log("[SEND] " + sender + " -> " + receiver + " | " + subject);
            return "OK|Email sent to " + toEmail;
        } catch (Exception e) {
            return "ERROR|" + e.getMessage();
        }
    }

    private String listUsers() {
        try {
            File root = new File(SERVER_DIR);
            File[] subs = root.listFiles();
            if (subs == null) return "OK|Empty";

            StringBuilder sb = new StringBuilder("OK|USERLIST\n");
            Arrays.sort(subs, Comparator.comparing(File::getName));
            for (File f : subs) {
                if (f.isDirectory() && new File(f, "new_email.txt").exists()) {
                    String email = f.getName() + "@gmail.com";
                    sb.append("USER|").append(f.getName()).append("|").append(email).append("\n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "ERROR|" + e.getMessage();
        }
    }

    private String extract(String content, String key) {
        for (String line : content.split("\n")) {
            if (line.startsWith(key + ":")) return line.substring(key.length() + 1).trim();
            if (line.startsWith(key)) {
                int idx = line.indexOf(':');
                if (idx > 0) return line.substring(idx + 1).trim();
            }
        }
        return "";
    }

    private String extractBody(String content) {
        int idx = content.indexOf("\n\n");
        if (idx > 0) return content.substring(idx + 2).trim();
        return "";
    }

    private static void write(File f, String data) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(f), "UTF-8"))) {
            bw.write(data);
        }
    }
    private static String read(File f) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(f), "UTF-8"))) {
            String l; while ((l = br.readLine()) != null) sb.append(l).append("\n");
        }
        return sb.toString();
    }
}