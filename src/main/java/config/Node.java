package config;

public class Node {
    String addr;
    int rmfPort;
//    int syncPort;
    int id;

    public String getAddr() {
        return addr;
    }

    public int getRmfPort() {
        return rmfPort;
    }

    public int getID() {
        return id;
    }

//    public int getSyncPort() {
//        return syncPort;
//    }

    public Node(String addr, int rmfPort, int id) {
        this.addr = addr;
        this.rmfPort = rmfPort;
//        this.syncPort = syncPort;
        this.id = id;
    }

//    public Node(Node n) {
//        new Node(n.getAddr(), n.getRmfPort(), n.getID());
//    }
}
