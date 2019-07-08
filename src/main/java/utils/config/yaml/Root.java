package utils.config.yaml;

public class Root {
    private String title;
        private int eVersion;
        private System system;
        private Settings settings;
        private ServerPrivateDetails server;
        private ServerPublicDetails[] cluster;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

        public int geteVersion() {
            return eVersion;
        }

        public void seteVersion(int eVersion) {
            this.eVersion = eVersion;
        }

        public Settings getSettings() {
            return settings;
        }

        public void setSettings(Settings settings) {
            this.settings = settings;
        }

        public System getSystem() {
            return system;
        }

        public void setSystem(System system) {
            this.system = system;
        }

        public ServerPrivateDetails getServer() {
            return server;
        }

        public void setServer(ServerPrivateDetails server) {
            this.server = server;
        }

        public ServerPublicDetails[] getCluster() {
            return cluster;
        }

        public void setCluster(ServerPublicDetails[] cluster) {
            this.cluster = cluster;
        }

    public Root(){}
}
