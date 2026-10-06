package client;

import java.net.*;

public class ClientCore {

    private String host = "localhost";
    private static final int PORT = 2023;

    private final DatagramSocket socket;

    public ClientCore() throws Exception {
        socket = new DatagramSocket();
        socket.setSoTimeout(3000);
    }

    public void setHost(String host) { this.host = host; }
    public String getHost()          { return host; }

    public String send(String req) throws Exception {
        InetAddress addr = InetAddress.getByName(host);

        // Retry 3 lần nếu packet mất
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