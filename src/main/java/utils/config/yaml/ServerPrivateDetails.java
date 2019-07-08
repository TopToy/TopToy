package utils.config.yaml;

public class ServerPrivateDetails {
    private String privateKey;
    private String tlsPrivKeyPath;
    private String tlsCertPath;

    public ServerPrivateDetails() {}

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public void setTlsPrivKeyPath(String tlsPrivKeyPath) {
        this.tlsPrivKeyPath = tlsPrivKeyPath;
    }

    public String getTlsPrivKeyPath() {
        return tlsPrivKeyPath;
    }

    public void setTlsCertPath(String tlsCertPath) {
        this.tlsCertPath = tlsCertPath;
    }

    public String getTlsCertPath() {
        return tlsCertPath;
    }

}
