package utils;

public class Node {
    private String addr;
    private int port;
    private int id;

    public String getAddr() {
        return addr;
    }

    public int getPort() {
        return port;
    }

    public int getID() {
        return id;
    }


    public Node(String addr, int rmfPort, int id) {
        this.addr = addr;
        this.port = rmfPort;
        this.id = id;
    }

}
