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


public class Config {
    static class tomlKeys {
        String SYSTEM_N_KEY = "system.n";
        String SYSTEM_F_KEY = "system.f";
        String SYSTEM_C_KEY = "system.c";
        String SYSTEM_TESTING_KEY = "system.testing";
        String SYSTEM_TX_SIZE = "system.txSize";
        String SYSTEM_CUTTER_BATCH = "system.cutterBatch";;
        String SERVER_CRT_PATH = "server.TlsCertPath";
        String CLUSTER_KEY = "cluster";
        String SETTING_TMO_KEY = "setting.tmo";
        String SETTING_TMO_INTERVAL_KEY = "setting.tmoInterval";
        String SETTING_ABCONFIG_KEY = "setting.ABConfigPath";
        String SETTING_MAXTRANSACTIONSINBLOCK_KEY = "setting.maxTransactionInBlock";
        String SETTING_CA_ROOT_PATH = "setting.caRootPath";
        String SETTING_FAST_MODE = "setting.fastMode";
        String SERVER_PRIVKEY = "server.privateKey";
        String SERVER_PRIVKET_PATH = "server.TlsPrivKeyPath";
    }

    private static tomlKeys tKeys;
    private static Toml conf;
    private static int s_id;
    private static Path tomlPath;

    private static org.apache.log4j.Logger logger; //= org.apache.log4j.Logger.getLogger(Config.class);
    public Config() {
//        Logger.getLogger("io.netty").setLevel(Level.OFF);
//        Logger.getLogger("io.grpc").setLevel(Level.OFF);
//        System.err.close();
        logger.debug("logger is configured");
    }

    public static void setConfig(Path path, int id) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy:hh:mm:ss");
        System.setProperty("current.date.time", dateFormat.format(new Date()));
        tomlPath =  Paths.get("src", "main", "resources", "config.toml");
        if (path != null) {
            tomlPath = path;
        }
        s_id = id;
        System.setProperty("s_id", Integer.toString(s_id));
        logger = org.apache.log4j.Logger.getLogger(Config.class);
        tKeys = new tomlKeys();
        conf = readConf(tomlPath);
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

    public static int getTMO() {
        return Math.toIntExact(conf.getLong(tKeys.SETTING_TMO_KEY));
    }

    public static int getTMOInterval() {
        return Math.toIntExact(conf.getLong(tKeys.SETTING_TMO_INTERVAL_KEY));
    }

    public static int getC() {
        return Math.toIntExact(conf.getLong(tKeys.SYSTEM_C_KEY));
    }

    public static boolean getTesting() {
        return conf.getBoolean(tKeys.SYSTEM_TESTING_KEY);
    }

    public static String getIP(int sID) {
        Toml t = conf.getTables(tKeys.CLUSTER_KEY).get(0);
        Toml node = t.getTable("s" + sID);
        return node.getString("ip");
    }

    public static ArrayList<Node> getWrbCluster() {
        Toml t = conf.getTables(tKeys.CLUSTER_KEY).get(0);
        ArrayList<Node> ret = new ArrayList<>();
        for (int i = 0 ; i < getN() ; i++) {
            Toml node = t.getTable("s" + i);
            ret.add(new Node(node.getString("ip"),
                    Math.toIntExact(node.getLong("wrbPort")),
                    Math.toIntExact(node.getLong("id"))));
        }
        return ret;
    }

    public static ArrayList<Node> getCommCluster() {
        Toml t = conf.getTables(tKeys.CLUSTER_KEY).get(0);
        ArrayList<Node> ret = new ArrayList<>();
        for (int i = 0 ; i < getN() ; i++) {
            Toml node = t.getTable("s" + i);
            ret.add(new Node(node.getString("ip"),
                    Math.toIntExact(node.getLong("commPort")),
                    Math.toIntExact(node.getLong("id"))));
        }
        return ret;
    }

    public static ArrayList<Node> getObbcCluster() {
        Toml t = conf.getTables(tKeys.CLUSTER_KEY).get(0);
        ArrayList<Node> ret = new ArrayList<>();
        for (int i = 0 ; i < getN() ; i++) {
            Toml node = t.getTable("s" + i);
            ret.add(new Node(node.getString("ip"),
                    Math.toIntExact(node.getLong("obbcPort")),
                    Math.toIntExact(node.getLong("id"))));
        }
        return ret;
    }

    public static HashMap<Integer, String> getClusterPubKeys() {
        Toml t = conf.getTables(tKeys.CLUSTER_KEY).get(0);
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

    public static String getABConfigHome() {
        return conf.getString(tKeys.SETTING_ABCONFIG_KEY);
    }

    public static String getPrivateKey() {
        return conf.getString(tKeys.SERVER_PRIVKEY);
    }

    public static String getServerPrivKeyPath() {
        return conf.getString(tKeys.SERVER_PRIVKET_PATH);
    }

    public static String getCaRootPath() {return conf.getString(tKeys.SETTING_CA_ROOT_PATH); }

    public static String getServerCrtPath() { return conf.getString(tKeys.SERVER_CRT_PATH); }

    public static boolean getFastMode() { return conf.getBoolean(tKeys.SETTING_FAST_MODE); }

    public static int getTxSize() {
        return Math.toIntExact(conf.getLong(tKeys.SYSTEM_TX_SIZE));
    }

    public static int getCutterBatch() {
        return Math.toIntExact(conf.getLong(tKeys.SYSTEM_CUTTER_BATCH));
    }


}
