package config;

public class Node {
    String addr;
    int port;
    int id;

    public String getAddr() {
        return addr;
    }

    public int getPort() {
        return port;
    }

    public int getID() {
        return id;
    }

    public Node(String addr, int port, int id) {
        this.addr = addr;
        this.port = port;
        this.id = id;
    }
}
