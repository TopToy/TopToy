package config;

public class Node {
    private String addr;
    private int rmfPort;
    private int id;

    public String getAddr() {
        return addr;
    }

    public int getRmfPort() {
        return rmfPort;
    }

    public int getID() {
        return id;
    }


    public Node(String addr, int rmfPort, int id) {
        this.addr = addr;
        this.rmfPort = rmfPort;
        this.id = id;
    }

}
