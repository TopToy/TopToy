import blockchain.asyncBcServer;
import blockchain.byzantineBcServer;
import blockchain.cbcServer;
import config.Node;
import org.apache.commons.lang.ArrayUtils;
import org.junit.jupiter.api.Test;
import serverGroup.sg;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import static config.Config.setConfig;

public class sgTest {
    private int timeToWaitBetweenTests = 1; //1000 * 60;
    //    static Config conf = new Config();
//    Config.setConfig(null, 0);
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(sgTest.class);
    private String localHost = "127.0.0.1";
    private int[] ports = {20000, 20010, 20020, 20030};
    //    private int[] syncPort = {30000, 30010, 30020, 30030};
    private ArrayList<Node> cluster = new ArrayList<Node>() {
        {
            add(new Node(localHost, ports[0], 0));
            add(new Node(localHost, ports[1], 1));
            add(new Node(localHost, ports[2], 2));
            add(new Node(localHost, ports[3], 3));
        }
    };
//    private ArrayList<Node> c1 = new ArrayList<Node>() {
//        {
//            add(new Node(localHost, ports[0][1], 0));
//            add(new Node(localHost, ports[1][1], 1));
//            add(new Node(localHost, ports[2][1], 2));
//            add(new Node(localHost, ports[3][1], 3));
//        }
//    };

    private Path singleBbc = Paths.get("Configurations", "single", "bbcConfig");
    private Path singlepanic = Paths.get("Configurations", "single", "panicRBConfig");
    private Path singlesync = Paths.get("Configurations", "single", "syncRBConfig");
    private Path singleConfig = Paths.get("Configurations", "single", "config.toml");

    private Path fourBbc = Paths.get("Configurations", "4Servers", "local", "bbcConfig");
    private Path fourpanic = Paths.get("Configurations", "4Servers", "local", "panicRBConfig");
    private Path foursync = Paths.get("Configurations", "4Servers", "local", "syncRBConfig");
    private Path fourConfig = Paths.get("Configurations", "4Servers", "local", "config.toml");

    private static void deleteViewIfExist(String configHome){
        File file2 = new File(Paths.get(configHome, "currentView").toString());
        if (file2.exists()) {
            logger.info("Saved view found in " + file2.getAbsolutePath() + " deleting...");
            if (file2.delete()) {
                logger.info(file2.getName() + " has been deleted");
            } else {
                logger.warn(file2.getName() + " could not be deleted");
            }
        }
    }


    private sg[] initLocalSgNodes(int nnodes, int f, int g, String bbcConfig, String panicConfig, String syncConfig, int[] byzIds) {
        deleteViewIfExist(bbcConfig);
        deleteViewIfExist(panicConfig);
        deleteViewIfExist(syncConfig);
//        ArrayList<ArrayList<Node>> clusters =  new ArrayList<>();
//        clusters.add(new ArrayList<>(c0.subList(0, nnodes)));
//        clusters.add(new ArrayList<>(c1.subList(0, nnodes)));
        sg[] ret = new sg[nnodes];
        for (int id = 0 ; id < nnodes ; id++) {
            logger.info("init server #" + id);
            if (ArrayUtils.contains(byzIds, id)) {
                ret[id] = new sg(localHost, ports[id], id, f, g, 1000,
                        100, 1, true, cluster, bbcConfig, panicConfig, syncConfig, "b");
                continue;
            }
            ret[id] = new sg(localHost, ports[id], id,f, g, 1000,
                    100, 1, true, cluster, bbcConfig, panicConfig, syncConfig, "r");
        }
        return ret;
        //        n.start();

    }

    private sg[] initLocalAsyncBCNodes(int nnodes, int f, int g, String bbcConfig, String panicConfig, String syncConfig, int[] byzIds) {
        deleteViewIfExist(bbcConfig);
        deleteViewIfExist(panicConfig);
        deleteViewIfExist(syncConfig);
//        ArrayList<ArrayList<Node>> clusters =  new ArrayList<>();
//        clusters.add((ArrayList<Node>) c0.subList(0, nnodes));
//        clusters.add((ArrayList<Node>) c1.subList(0, nnodes));
        sg[] ret = new sg[nnodes];
        for (int id = 0 ; id < nnodes ; id++) {
            logger.info("init server #" + id);
            if (ArrayUtils.contains(byzIds, id)) {
                ret[id] = new sg(localHost, ports[id], id,f, g, 1000,
                        100, 1, true, cluster, bbcConfig, panicConfig, syncConfig, "b");
                continue;
            }
            ret[id] = new sg(localHost, ports[id], id,f, g, 1000,
                    100, 1, true, cluster, bbcConfig, panicConfig, syncConfig, "a");
        }
        return ret;
    }

    @Test
    void TestSingleServer() throws InterruptedException {
        setConfig(singleConfig, 0);
        Thread.sleep(timeToWaitBetweenTests);
        logger.info("start TestSingleServer");
        sg[] rn1 = initLocalSgNodes(1, 0, 2, singleBbc.toString(),
                singlepanic.toString(), singlesync.toString(), null);
        rn1[0].start();
        rn1[0].shutdown();
    }
}
