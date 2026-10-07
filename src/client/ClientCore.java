package client;

import java.net.*;

public class ClientCore {

    // ═══════════════════════════════════════════════════════════
    //  ⭐ IP SERVER — HARDCODE TRỰC TIẾP TRONG CODE
    //  Đổi IP này khi máy server đổi WiFi
    // ═══════════════════════════════════════════════════════════
    private static final String SERVER_IP = "172.26.22.8";
    private static final int    PORT      = 2023;

    private final DatagramSocket socket;

    public ClientCore() throws Exception {
        socket = new DatagramSocket();
        socket.setSoTimeout(3000);
    }

    // ⭐ 2 HÀM ĐANG THIẾU
    public String getServerIP() { return SERVER_IP; }
    public int    getPort()     { return PORT; }

    public String send(String req) throws Exception {
        InetAddress addr = InetAddress.getByName(SERVER_IP);

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                byte[] out = req.getBytes("UTF-8");
                socket.send(new DatagramPacket(out, out.length, addr, PORT));

                byte[] in = new byte[16384];
                DatagramPacket rp = new DatagramPacket(in, in.length);
                socket.receive(rp);

                return new String(rp.getData(), 0, rp.getLength(), "UTF-8");
            } catch (java.net.SocketTimeoutException te) {
                if (attempt == 3) throw te;
            }
        }
        throw new Exception("Send failed after 3 attempts");
    }

    public void close() { socket.close(); }
}