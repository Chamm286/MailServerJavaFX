package client;

import java.net.*;

public class ClientCore {

    private String host = "localhost";
    private static final int PORT = 2023;

    private final DatagramSocket socket;

    public ClientCore() throws Exception {
        socket = new DatagramSocket();
    }

    public void setHost(String host) { this.host = host; }
    public String getHost()          { return host; }

    public String send(String req) throws Exception {
        InetAddress addr = InetAddress.getByName(host);

        byte[] out = req.getBytes("UTF-8");
        socket.send(new DatagramPacket(out, out.length, addr, PORT));

        socket.setSoTimeout(5000);

        byte[] in = new byte[8192];
        DatagramPacket rp = new DatagramPacket(in, in.length);
        socket.receive(rp);

        return new String(rp.getData(), 0, rp.getLength(), "UTF-8");
    }

    public void close() { socket.close(); }
}