package utils.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import das.ab.ABService;
import das.bbc.BBC;
import das.bbc.OBBC;
import das.ms.Membership;
import das.wrb.WRB;
import servers.Top;
import utils.config.yaml.Root;
import utils.config.yaml.ServerPublicDetails;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;



public class Config {

    private static final Object gLock = new Object();
    private static int s_id;
    private static Path yamlPath;
    private static Root configration;
    private static org.apache.log4j.Logger logger;
    public Config() {
        logger.debug("logger is configured");
    }

    public static void setConfig(Path path, int id) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy:hh:mm:ss");
        System.setProperty("current.date.time", dateFormat.format(new Date()));
        yamlPath =  Paths.get("src", "main", "resources", "config.yaml");
        if (path != null) {
            yamlPath = path;
        }
        s_id = id;
        System.setProperty("s_id", Integer.toString(s_id));
        logger = org.apache.log4j.Logger.getLogger(Config.class);
        readConf(yamlPath);
    }

    private static void readConf(Path yamlPath) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            configration = mapper.readValue(new File(yamlPath.toString()), Root.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static int getN() {
        synchronized (gLock) {
           return configration.getSystem().getN();
        }
    }

    public static int getF() {
        synchronized (gLock) {
            return configration.getSystem().getF();
        }
    }

    public static int getTMO() {
        synchronized (gLock) {
           return configration.getSettings().getTmo();
        }
    }

    public static int getC() {
        synchronized (gLock) {
           return configration.getSystem().getW();
        }
    }

    public static boolean getTesting() {
        synchronized (gLock) {
            return configration.getSystem().getTesting() == 1;
        }
    }

    public static String getIP(int sID) {
        synchronized (gLock) {
            return configration.getCluster()[sID].getIp();
        }

    }

    public static ServerPublicDetails[] getCluster() {
        synchronized (gLock) {
            return configration.getCluster();
        }
    }

    public static HashMap<Integer, String> getClusterPubKeys() {
        synchronized (gLock) {
            HashMap<Integer, String> ret = new HashMap<>();
            for (ServerPublicDetails s : getCluster()) {
                ret.put(s.getId(), s.getPublicKey());

            }
            return ret;
        }

    }
    public static int getMaxTransactionsInBlock() {
        synchronized (gLock) {
            return configration.getSettings().getMaxTransactionInBlock();
        }
    }

    public static String getABConfigHome() {
        synchronized (gLock) {
           return configration.getSettings().getAtomicbroadcast();
        }
    }

    public static String getPrivateKey() {
        synchronized (gLock) {
            return configration.getServer().getPrivateKey();
        }
    }

    public static String getServerPrivKeyPath() {
        synchronized (gLock) {
            return configration.getServer().getTlsPrivKeyPath();
        }
    }

    public static String getCaRootPath() {
        synchronized (gLock) {
            return configration.getSettings().getCaRootPath();
        }
    }

    public static String getServerCrtPath() {
        synchronized (gLock) {
            return configration.getServer().getTlsCertPath();
        }
    }

    public static int getTxSize() {
        synchronized (gLock) {
            return configration.getSettings().getTxSize();
        }
    }

    // TODO: What about race conditions? on which point we should reconfigure the system?
    public static void reconfigure() {
            Top.reconfigure();
            ABService.reconfigure();
            BBC.reconfigure();
            OBBC.reconfigure();
            WRB.reconfigure();
            Membership.reconfigure();
    }

    static void setN(int n) {
        configration.getSystem().setN(n);
    }


    static void setF(int f) {
        configration.getSystem().setF(f);
    }

    static void writeConfiguration() {
        // Create an ObjectMapper mapper for YAML
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

// Write object as YAML file
        try {
            mapper.writeValue(new File(yamlPath.toString()), configration);
        } catch (IOException e) {
            logger.error(e);
        }
    }

}
