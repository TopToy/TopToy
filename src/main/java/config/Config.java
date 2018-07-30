package config;

import com.moandjiezana.toml.Toml;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


public class Config {
    static class tomlKeys {
        String SYSTEM_KEY = "system";
        String SYSTEM_N_KEY = "system.n";
        String SYSTEM_F_KEY = "system.f";
        String SERVER_KEY = "server";
        String SERVER_ID_KEY = "server.id";
        String SERVER_IP_KEY = "server.ip";
        String SERVER_PORT_KEY = "server.port";
        String RMFCLUSTER_KEY = "RMFcluster";
        String RMFCLUSTER_SERVER_KEY = "RMFcluster.s";
        String SETTING_KEY = "setting";
        String SETTING_TMO_KEY = "setting.tmo";
        String SETTING_TMO_INTERVAL_KEY = "setting.tmoInterval";
        String SETTING_RMFBBCCONFIG_KEY = "setting.rmfBbcConfigPath";
        String SETTING_RBROADACSTCONFIG_PATH = "RBroadcastConfigPath";
        String SETTING_MAXTRANSACTIONSINBLOCK_KEY = "setting.maxTransactionInBlock";
        String SERVER_PRIVKEY = "server.privateKey";
        String SERVER_PUBKEY = "server.publicKey";
    }

    private static tomlKeys tKeys;
    private static Toml conf;
    private static Path tomlPath = Paths.get("src", "main", "resources", "config.toml");
    static {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy:hh:m");
        System.setProperty("current.date.time", dateFormat.format(new Date()));
        tKeys = new tomlKeys();
        conf = readConf(tomlPath);

    }

    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Config.class);
    public Config() {
        logger.debug("logger is configured");
    }

    static Toml readConf(Path tomlPath) {
        byte[] encoded = new byte[0];
        try {
            encoded = Files.readAllBytes(tomlPath);
        } catch (IOException e) {
            logger.fatal("", e);
        }
        String content = new String(encoded, Charset.defaultCharset());
        return new Toml().read(content);

    }

    public static int getN() {
        return Math.toIntExact(conf.getLong(tKeys.SYSTEM_N_KEY));
    }

    public static int getF() {
        return Math.toIntExact(conf.getLong(tKeys.SYSTEM_F_KEY));
    }

    public static Node getNodeConf() {
        return new Node(conf.getString(tKeys.SERVER_IP_KEY),
                Math.toIntExact(conf.getLong(tKeys.SERVER_PORT_KEY)),
                Math.toIntExact(conf.getLong(tKeys.SERVER_ID_KEY)));
    }

    public static int getTMO() {
        return Math.toIntExact(conf.getLong(tKeys.SETTING_TMO_KEY));
    }

    public static int getTMOInterval() {
        return Math.toIntExact(conf.getLong(tKeys.SETTING_TMO_INTERVAL_KEY));
    }

    public static ArrayList<Node> getRMFcluster() {
        Toml t = conf.getTables(tKeys.RMFCLUSTER_KEY).get(0);
        ArrayList<Node> ret = new ArrayList<>();
        for (int i = 0 ; i < getN() ; i++) {
            Toml node = t.getTable("s" + i);
            ret.add(new Node(node.getString("ip"),
                    Math.toIntExact(node.getLong("port")),
                    Math.toIntExact(node.getLong("id"))));
        }
        return ret;
    }

    public static HashMap<Integer, String> getClusterPubKeys() {
        Toml t = conf.getTables(tKeys.RMFCLUSTER_KEY).get(0);
        HashMap<Integer, String> ret = new HashMap<>();
        for (int i = 0 ; i < getN() ; i++) {
            Toml node = t.getTable("s" + i);
            ret.put(Math.toIntExact(node.getLong("id")), node.getString("publicKey"));
        }
        return ret;
    }
    public static int getMaxTransactionsInBlock() {
        return Math.toIntExact(conf.getLong(tKeys.SETTING_MAXTRANSACTIONSINBLOCK_KEY));
    }

    public static String getRMFbbcConfigHome() {
        return conf.getString(tKeys.SETTING_RMFBBCCONFIG_KEY);
    }

    public static String getRBroadcastConfigHome() {
        return conf.getString(tKeys.SETTING_RBROADACSTCONFIG_PATH);
    }

    public static String getPublicKey() {
        return conf.getString(tKeys.SERVER_PUBKEY);
    }


    public static String getPrivateKey() {
        return conf.getString(tKeys.SERVER_PRIVKEY);
    }


}
