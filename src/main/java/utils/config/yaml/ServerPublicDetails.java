package utils.config.yaml;

public class ServerPublicDetails {
    private int id;
    private String ip;
    private int wrbPort;
    private int commPort;
    private int obbcPort;
    private String publicKey;

    public ServerPublicDetails() {}

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public int getWrbPort() {
        return wrbPort;
    }

    public void setWrbPort(int wrbPort) {
        this.wrbPort = wrbPort;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getCommPort() {
        return commPort;
    }

    public int getObbcPort() {
        return obbcPort;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setCommPort(int commPort) {
        this.commPort = commPort;
    }

    public void setObbcPort(int obbcPort) {
        this.obbcPort = obbcPort;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

}
