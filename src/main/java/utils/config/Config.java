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
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;


public class Config {

//    private static final Object gLock = new Object();
    private static int s_id;
    private static Path yamlDirPath;
    private static ConcurrentHashMap<Integer, Root> configurations = new ConcurrentHashMap<>();
    private static org.apache.log4j.Logger logger;
//    private static int currVersion = 0;
    private static String configFileName = "config.yaml";
    public Config() {
        logger.debug("logger is configured");
    }

    public static void setConfig(Path path, int id) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy:hh:mm:ss");
        System.setProperty("current.date.time", dateFormat.format(new Date()));
        yamlDirPath =  Paths.get("src", "main", "resources");
        if (path != null) {
            yamlDirPath = path;
        }
        s_id = id;
        System.setProperty("s_id", Integer.toString(s_id));
        logger = org.apache.log4j.Logger.getLogger(Config.class);
        readConf();
    }

    private static void readConf() {
        Path cp = Paths.get(yamlDirPath.toString(), configFileName);
        if (version() + 1 > 0) {
            cp = Paths.get(yamlDirPath.toString(), format("config_%d.yaml", version()));
        }
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            configurations.put(version() + 1, mapper.readValue(new File(cp.toString()), Root.class));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static int getN(int version) {
       return configurations.get(version).getSystem().getN();
    }

    public static int getF(int version) {
        return configurations.get(version).getSystem().getF();
    }

    public static int getN() {
        return configurations.get(version()).getSystem().getN();
    }

    public static int getF() {
        return configurations.get(version()).getSystem().getF();
    }

    public static int getTMO() {
       return configurations.get(version()).getSettings().getTmo();
    }

    public static int getC() {
       return configurations.get(version()).getSystem().getW();
    }

    public static boolean getTesting() {
        return configurations.get(version()).getSystem().getTesting() == 1;
    }

    public static String getIP(int sID) {
        return configurations.get(version()).getCluster()[sID].getIp();
    }

    public static ServerPublicDetails[] getCluster() {
        return configurations.get(version()).getCluster();
    }

    public static HashMap<Integer, String> getClusterPubKeys() {
            HashMap<Integer, String> ret = new HashMap<>();
            for (ServerPublicDetails s : getCluster()) {
                ret.put(s.getId(), s.getPublicKey());

            }
            return ret;
    }

    public static int getMaxTransactionsInBlock() {
        return configurations.get(version()).getSettings().getMaxTransactionInBlock();
    }

    public static String getABConfigHome() {
        return configurations.get(version()).getSettings().getAtomicbroadcast();
    }

    public static String getPrivateKey() {
        return configurations.get(version()).getServer().getPrivateKey();
    }

    public static String getServerPrivKeyPath() {
        return configurations.get(version()).getServer().getTlsPrivKeyPath();
    }

    public static String getCaRootPath() {
        return configurations.get(version()).getSettings().getCaRootPath();
    }

    public static String getServerCrtPath() {
        return configurations.get(version()).getServer().getTlsCertPath();
    }

    public static int getTxSize() {
        return configurations.get(version()).getSettings().getTxSize();
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

    static int version() {
        return configurations.size() - 1;
    }

    static public void updateConfiguration(int n, int f) {
        Root newConf = configurations.get(version());
        newConf.getSystem().setN(n);
        newConf.getSystem().setF(f);
        writeConfiguration(newConf);
    }
    static void writeConfiguration(Root newConf) {
        Path cp = Paths.get(yamlDirPath.toString(), format("config_%d.yaml", version() + 1));
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            mapper.writeValue(new File(cp.toString()), newConf);
        } catch (IOException e) {
            logger.error(e);
        }
    }

}
