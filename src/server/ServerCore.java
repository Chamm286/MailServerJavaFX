package server;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ServerCore {

    public static final String SERVER_DIR = "F:/server_mail";
    public static final String DOMAIN     = "gmail.com";

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
                logger.log("[DOMAIN] @" + DOMAIN);

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
                if (p.length < 4) return "ERROR|Missing params";
                return getMailContent(p[1], p[2], p[3]);
            case "STAR":
                if (p.length < 4) return "ERROR|Missing params";
                return toggleStar(p[1], p[2], p[3]);
            case "DELETE":
                if (p.length < 4) return "ERROR|Missing params";
                return deleteMail(p[1], p[2], p[3]);
            case "SENT":
                if (p.length < 2) return "ERROR|Missing user";
                return listSent(p[1]);
            case "TRASH":
                if (p.length < 2) return "ERROR|Missing user";
                return listTrash(p[1]);
            case "STARRED":
                if (p.length < 2) return "ERROR|Missing user";
                return listStarred(p[1]);
            default:
                return "ERROR|Unknown command";
        }
    }

    private String register(String user, String pass) {
        try {
            File dir = new File(SERVER_DIR, user);
            if (dir.exists()) return "ERROR|Account already exists";
            dir.mkdirs();
            new File(dir, "inbox").mkdirs();
            new File(dir, "sent").mkdirs();
            new File(dir, "trash").mkdirs();

            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            String email = user + "@" + DOMAIN;

            write(new File(dir, "user.txt"),  user);
            write(new File(dir, "pass.txt"),  pass);
            write(new File(dir, "date.txt"),  now);
            write(new File(dir, "email.txt"), email);
            write(new File(dir, "starred.txt"), "");

            write(new File(dir, "new_email.txt"),
                  "Thank you for using this service. we hope that you will feel comfortable\n" +
                  "using our mail system.\n" +
                  "-------------------------------------------------\n" +
                  "User    : " + user + "\n" +
                  "Email   : " + email + "\n" +
                  "Created : " + now  + "\n");

            write(new File(new File(dir, "inbox"), "welcome.txt"),
                  "From          : server@" + DOMAIN + "\n" +
                  "To            : " + email + "\n" +
                  "IP nguoi gui  : 127.0.0.1\n" +
                  "Thoi gian gui : " + now + "\n" +
                  "Tieu de       : Welcome to Mail Server\n" +
                  "Noi dung      : Thank you for using this service. we hope that you will feel comfortable using our mail system.\n");

            logger.log("[OK] Registered: " + email);
            return "OK|Register successful. Your email: " + email;
        } catch (Exception e) {
            return "ERROR|" + e.getMessage();
        }
    }

    private String login(String user, String pass) {
        try {
            File dir = new File(SERVER_DIR, user);
            if (!dir.exists()) return "ERROR|Account not found";

            String sU = read(new File(dir, "user.txt")).trim();
            String sP = read(new File(dir, "pass.txt")).trim();

            if (!sU.equals(user) || !sP.equals(pass))
                return "ERROR|Wrong username or password";

            String myEmail = read(new File(dir, "email.txt")).trim();

            StringBuilder sb = new StringBuilder("OK|Login successful\n");
            sb.append("EMAIL|").append(myEmail).append("\n");
            sb.append("SECTION|INBOX\n");

            appendAccountFiles(sb, dir, user);
            appendMailList(sb, new File(dir, "inbox"), user, dir, "INBOX");

            return sb.toString();
        } catch (Exception e) {
            return "ERROR|" + e.getMessage();
        }
    }

    private void appendAccountFiles(StringBuilder sb, File dir, String user) {
        File[] files = dir.listFiles();
        if (files == null) return;
        Arrays.sort(files, Comparator.comparing(File::getName));
        for (File f : files) {
            if (f.isFile()) {
                sb.append("ACCOUNTFILE|").append(f.getName()).append("\n");
            }
        }
    }

    private String listSent(String user) {
        try {
            File dir = new File(SERVER_DIR, user);
            if (!dir.exists()) return "ERROR|Account not found";
            StringBuilder sb = new StringBuilder("OK|SENT\n");
            sb.append("SECTION|SENT\n");
            appendMailList(sb, new File(dir, "sent"), user, dir, "SENT");
            return sb.toString();
        } catch (Exception e) {
            return "ERROR|" + e.getMessage();
        }
    }

    private String listTrash(String user) {
        try {
            File dir = new File(SERVER_DIR, user);
            if (!dir.exists()) return "ERROR|Account not found";
            StringBuilder sb = new StringBuilder("OK|TRASH\n");
            sb.append("SECTION|TRASH\n");
            appendMailList(sb, new File(dir, "trash"), user, dir, "TRASH");
            return sb.toString();
        } catch (Exception e) {
            return "ERROR|" + e.getMessage();
        }
    }

    private String listStarred(String user) {
        try {
            File dir = new File(SERVER_DIR, user);
            if (!dir.exists()) return "ERROR|Account not found";

            File starredFile = new File(dir, "starred.txt");
            Set<String> starred = new HashSet<>();
            if (starredFile.exists()) {
                for (String line : read(starredFile).split("\n")) {
                    String s = line.trim();
                    if (!s.isEmpty()) starred.add(s);
                }
            }

            StringBuilder sb = new StringBuilder("OK|STARRED\n");
            sb.append("SECTION|STARRED\n");

            File[] folders = { new File(dir, "inbox"), new File(dir, "sent"), new File(dir, "trash") };
            for (File folder : folders) {
                if (!folder.exists()) continue;
                File[] files = folder.listFiles();
                if (files == null) continue;
                for (File f : files) {
                    if (starred.contains(f.getName())) {
                        appendOneMail(sb, f, folder.getName(), user);
                    }
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "ERROR|" + e.getMessage();
        }
    }

    private String getMailContent(String user, String folder, String fileName) {
        try {
            File dir = new File(SERVER_DIR, user);
            File mail = new File(new File(dir, folder), fileName);
            if (!mail.exists()) return "ERROR|Mail not found";
            String content = read(mail);
            return "OK|" + content;
        } catch (Exception e) {
            return "ERROR|" + e.getMessage();
        }
    }

    private String toggleStar(String user, String folder, String fileName) {
        try {
            File dir = new File(SERVER_DIR, user);
            File starredFile = new File(dir, "starred.txt");
            Set<String> starred = new LinkedHashSet<>();
            if (starredFile.exists()) {
                for (String line : read(starredFile).split("\n")) {
                    String s = line.trim();
                    if (!s.isEmpty()) starred.add(s);
                }
            }

            boolean wasStarred = starred.contains(fileName);
            if (wasStarred) starred.remove(fileName);
            else starred.add(fileName);

            StringBuilder sb = new StringBuilder();
            for (String s : starred) sb.append(s).append("\n");
            write(starredFile, sb.toString());

            return "OK|" + (wasStarred ? "Unstarred" : "Starred");
        } catch (Exception e) {
            return "ERROR|" + e.getMessage();
        }
    }

    private String deleteMail(String user, String folder, String fileName) {
        try {
            File dir = new File(SERVER_DIR, user);
            File src = new File(new File(dir, folder), fileName);
            if (!src.exists()) return "ERROR|Mail not found";

            File trash = new File(dir, "trash");
            trash.mkdirs();
            File dst = new File(trash, fileName);

            if (folder.equals("trash")) {
                src.delete();
                return "OK|Permanently deleted";
            }

            java.nio.file.Files.move(src.toPath(), dst.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return "OK|Moved to trash";
        } catch (Exception e) {
            return "ERROR|" + e.getMessage();
        }
    }

    private void appendMailList(StringBuilder sb, File folder, String user, File dir, String folderName) {
        if (!folder.exists()) return;
        File[] files = folder.listFiles();
        if (files == null) return;

        Arrays.sort(files, Comparator.comparing(File::getName).reversed());

        Set<String> starred = new HashSet<>();
        File sf = new File(dir, "starred.txt");
        if (sf.exists()) {
            try {
                for (String line : read(sf).split("\n")) {
                    String s = line.trim();
                    if (!s.isEmpty()) starred.add(s);
                }
            } catch (Exception ignored) {}
        }

        for (File f : files) {
            if (!f.isFile()) continue;
            try {
                String content = read(f);
                String from = extract(content, "From");
                String to   = extract(content, "To");
                String time = extract(content, "Thoi gian gui");
                String subj = extract(content, "Tieu de");
                String body = extract(content, "Noi dung");

                boolean isStar = starred.contains(f.getName());

                sb.append("MAIL|")
                  .append(folderName).append("|")
                  .append(f.getName()).append("|")
                  .append(from).append("|")
                  .append(to).append("|")
                  .append(subj).append("|")
                  .append(time).append("|")
                  .append(body.replace("\n", " ")).append("|")
                  .append(isStar ? "1" : "0").append("\n");
            } catch (Exception ignored) {}
        }
    }

    private void appendOneMail(StringBuilder sb, File f, String folderName, String user) {
        try {
            File dir = new File(SERVER_DIR, user);
            String content = read(f);
            String from = extract(content, "From");
            String to   = extract(content, "To");
            String time = extract(content, "Thoi gian gui");
            String subj = extract(content, "Tieu de");
            String body = extract(content, "Noi dung");

            File sf = new File(dir, "starred.txt");
            Set<String> starred = new HashSet<>();
            if (sf.exists()) {
                for (String line : read(sf).split("\n")) {
                    if (!line.trim().isEmpty()) starred.add(line.trim());
                }
            }
            boolean isStar = starred.contains(f.getName());

            sb.append("MAIL|")
              .append(folderName).append("|")
              .append(f.getName()).append("|")
              .append(from).append("|")
              .append(to).append("|")
              .append(subj).append("|")
              .append(time).append("|")
              .append(body.replace("\n", " ")).append("|")
              .append(isStar ? "1" : "0").append("\n");
        } catch (Exception ignored) {}
    }

    private String sendMail(String sender, String fromEmail, String toEmail,
                            String subject, String content,
                            InetAddress senderIP) {
        try {
            String receiver = toEmail.contains("@")
                    ? toEmail.substring(0, toEmail.indexOf('@'))
                    : toEmail;

            File rDir = new File(SERVER_DIR, receiver);
            if (!rDir.exists()) return "ERROR|Receiver does not exist";

            File senderDir = new File(SERVER_DIR, sender);

            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
            String fname = "email_" + sender + "_" + ts + ".txt";

            StringBuilder body = new StringBuilder();
            body.append("From          : ").append(fromEmail).append("\n");
            body.append("To            : ").append(toEmail).append("\n");
            body.append("IP nguoi gui  : ").append(senderIP.getHostAddress()).append("\n");
            body.append("Thoi gian gui : ").append(new Date()).append("\n");
            body.append("Tieu de       : ").append(subject).append("\n");
            body.append("Noi dung      : ").append(content).append("\n");

            File receiverInbox = new File(rDir, "inbox");
            receiverInbox.mkdirs();
            write(new File(receiverInbox, fname), body.toString());

            File senderSent = new File(senderDir, "sent");
            senderSent.mkdirs();
            write(new File(senderSent, fname), body.toString());

            logger.log("[SEND] " + fromEmail + " -> " + toEmail);
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
                if (f.isDirectory() && new File(f, "email.txt").exists()) {
                    String email = read(new File(f, "email.txt")).trim();
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
            if (line.startsWith(key)) {
                int idx = line.indexOf(':');
                if (idx > 0) return line.substring(idx + 1).trim();
            }
        }
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