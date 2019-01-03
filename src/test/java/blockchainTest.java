import servers.*;

import config.Config;
import config.Node;
import org.apache.commons.lang.ArrayUtils;
import org.junit.jupiter.api.Test;

import proto.Types.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static config.Config.setConfig;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class blockchainTest {

    private int timeToWaitBetweenTests = 1; //1000 * 60;
//    static Config conf = new Config();
//    Config.setConfig(null, 0);
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(blockchainTest.class);
    private String localHost = "127.0.0.1";
    private int[] rmfPorts = {20000, 20010, 20020, 20030};
//    private int[] syncPort = {30000, 30010, 30020, 30030};
    private Node s0= new Node(localHost, rmfPorts[0],  0);
    private Node s1= new Node(localHost, rmfPorts[1],  1);
    private Node s2= new Node(localHost, rmfPorts[2],  2);
    private Node s3= new Node(localHost, rmfPorts[3],  3);
    private ArrayList<Node> nodes = new ArrayList<Node>() {{add(s0); add(s1); add(s2); add(s3);}};

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


    private Node[] initLocalBCNodes(int nnodes, int f, String bbcConfig, String panicConfig, String syncConfig, int[] byzIds) {
        deleteViewIfExist(bbcConfig);
        deleteViewIfExist(panicConfig);
        deleteViewIfExist(syncConfig);
        ArrayList<Node> currentNodes = new ArrayList<>(nodes.subList(0, nnodes));
        Node[] ret = new Node[nnodes];
        for (int id = 0 ; id < nnodes ; id++) {
            logger.info("init server #" + id);
            if (ArrayUtils.contains(byzIds, id)) {
                ret[id] = new byzantineBcServer(localHost, rmfPorts[id], id, 0, f, 1000, 100,
                        1, true, currentNodes, bbcConfig, panicConfig, syncConfig,
                        Config.getServerCrtPath(), Config.getServerTlsPrivKeyPath(), Config.getCaRootPath());
                continue;
            }
            ret[id] =  new cbcServer(localHost, rmfPorts[id], id, 0, f, 1000, 100,
                    1, true, currentNodes, bbcConfig, panicConfig, syncConfig,
                    Config.getServerCrtPath(), Config.getServerTlsPrivKeyPath(), Config.getCaRootPath());
        }
        return ret;
        //        n.start();

    }

    private Node[] initLocalAsyncBCNodes(int nnodes, int f, String bbcConfig, String panicConfig, String syncConfig, int[] byzIds) {
        deleteViewIfExist(bbcConfig);
        deleteViewIfExist(panicConfig);
        deleteViewIfExist(syncConfig);
        ArrayList<Node> currentNodes = new ArrayList<>(nodes.subList(0, nnodes));
        Node[] ret = new Node[nnodes];
        for (int id = 0 ; id < nnodes ; id++) {
            if (ArrayUtils.contains(byzIds, id)) {
                ret[id] = new byzantineBcServer(localHost, rmfPorts[id], id, 0, f, 1000, 100,
                        1, true, currentNodes, bbcConfig, panicConfig, syncConfig,
                        Config.getServerCrtPath(), Config.getServerTlsPrivKeyPath(), Config.getCaRootPath());
                continue;
            }
            logger.info("init server #" + id);
            ret[id] =  new asyncBcServer(localHost, rmfPorts[id], id, 0, f, 1000, 100,
                    1, true, currentNodes, bbcConfig, panicConfig, syncConfig,
                    Config.getServerCrtPath(), Config.getServerTlsPrivKeyPath(), Config.getCaRootPath());
        }
        return ret;
        //        n.start();

    }

    @Test
    void TestSingleServer() throws InterruptedException {
        setConfig(singleConfig, 0);
        Thread.sleep(timeToWaitBetweenTests);
        logger.info("start TestSingleServer");
        Node[] rn1 = initLocalBCNodes(1, 0, singleBbc.toString(),
                singlepanic.toString(), singlesync.toString(), null);
        ((cbcServer) rn1[0]).start(false);
        ((cbcServer) rn1[0]).shutdown(false);
    }
//
    @Test
    void TestSingleServerDisseminateMessage() throws InterruptedException {
        setConfig(null, 0);
        Thread.sleep(timeToWaitBetweenTests);
        logger.info("start TestSingleServer");
        Node[] rn1 = initLocalBCNodes(1, 0, singleBbc.toString(),
                singlepanic.toString(), singlesync.toString(), null);
        ((cbcServer) rn1[0]).start(false);
        ((cbcServer) rn1[0]).serve();
        for (int i = 0 ; i < 1000 ; i++) {
            ((cbcServer) rn1[0]).addTransaction(("hello").getBytes(), 100);
        }
        Thread.sleep(30 * 1000);
        ((cbcServer) rn1[0]).shutdown(false);

    }

    @Test
    void TestFourServersNoFailuresSingleMessage() throws InterruptedException {
        setConfig(fourConfig, 0);
        Thread.sleep(timeToWaitBetweenTests);
        int nnodes = 4;
        logger.info("start TestFourServersNoFailures");
        Thread[] servers = new Thread[4];
        Node[] allNodes = initLocalBCNodes(nnodes,1, fourBbc.toString(),
                fourpanic.toString(), foursync.toString(), null);
        for (int i = 0 ; i < nnodes ; i++) {
            int finalI = i;
            servers[i]  = new Thread(() ->((cbcServer) allNodes[finalI]).start(false));
            servers[i].start();
        }
        for (int i = 0 ; i < nnodes ; i++) {
            servers[i].join();
        }
        String msg = "Hello";
        ((cbcServer) allNodes[0]).addTransaction(msg.getBytes(), 0);
        for (int i = 0 ; i < nnodes ; i++) {
            ((cbcServer) allNodes[i]).serve();
        }
        String[] ret = new String[4];
        Thread[] tasks = new Thread[4];
        for (int i = 0 ; i < nnodes ; i++) {
            int finalI = i;
            tasks[i] = new Thread(()-> {
                try {
                    ret[finalI] = new String(((cbcServer) allNodes[finalI]).deliver(1).getData(0).getData().toByteArray());
                } catch (InterruptedException e) {
                    logger.error("", e);
                }
            });
            tasks[i].start();
        }

        for (int i = 0 ; i < nnodes ; i++) {
            tasks[i].join();
        }
        for (int i = 0 ; i < 4 ; i++) {
            ((cbcServer) allNodes[i]).shutdown(true);
        }
        for (int i = 0 ; i < 4 ; i++) {
            assertEquals(msg, ret[i]);
//            ((RmfNode) allNodes[i]).stop();
        }

    }

//    @Test
//    void TestFourbftSmartServersNoFailuresSingleMessage() throws InterruptedException {
//        setConfig(null, 0);
//        Thread.sleep(timeToWaitBetweenTests);
//        int nnodes = 4;
//        logger.info("start TestFourServersNoFailures");
//        Thread[] servers = new Thread[4];
//        bftsmartBCserver[] allNodes = new bftsmartBCserver[4];
//        for (int i = 0 ; i < 4 ; i++) {
//            allNodes[i] = new bftsmartBCserver(i, 1, 1, Config.getSyncRBConfigHome());
//        }
//        for (int i = 0 ; i < nnodes ; i++) {
//            int finalI = i;
//            servers[i]  = new Thread(() ->(allNodes[finalI]).start());
//            servers[i].start();
//        }
//        for (int i = 0 ; i < nnodes ; i++) {
//            servers[i].join();
//        }
//        String msg = "Hello";
//        for (int i = 0 ; i < 100000 ; i++) {
//            (allNodes[i % 4]).addTransaction((msg + i).getBytes(), 0);
//        }
//
//        for (int i = 0 ; i < nnodes ; i++) {
//            (allNodes[i]).serve();
//        }
//        String[] ret = new String[4];
//        Thread[] tasks = new Thread[4];
//        for (int i = 0 ; i < nnodes ; i++) {
//            int finalI = i;
//            tasks[i] = new Thread(()-> {
//                try {
//                    ret[finalI] = new String((allNodes[finalI]).deliver(1).getData(0).getData().toByteArray());
//                } catch (InterruptedException e) {
//                    logger.error("", e);
//                }
//            });
//            tasks[i].start();
//        }
//        Thread.sleep(10 * 1000);
//        for (int i = 0 ; i < nnodes ; i++) {
//            tasks[i].join();
//        }
//        for (int i = 0 ; i < 4 ; i++) {
//            ( allNodes[i]).shutdown();
//        }
////        for (int i = 0 ; i < 4 ; i++) {
////            assertEquals(msg, ret[i]);
//////            ((RmfNode) allNodes[i]).stop();
////        }
//
//    }
//
//@Test
//void TestFourhlfNoFailuresSingleMessage() throws InterruptedException {
//    setConfig(null, 0);
//    Thread.sleep(timeToWaitBetweenTests);
//    int nnodes = 4;
//    logger.info("start TestFourServersNoFailures");
//    Thread[] servers = new Thread[4];
//    hlfBCserver[] allNodes = new hlfBCserver[4];
//    for (int i = 0 ; i < 4 ; i++) {
//        allNodes[i] = new hlfBCserver(i, 1, 1, Config.getSyncRBConfigHome());
//    }
//    for (int i = 0 ; i < nnodes ; i++) {
//        int finalI = i;
//        servers[i]  = new Thread(() ->(allNodes[finalI]).start());
//        servers[i].start();
//    }
//    for (int i = 0 ; i < nnodes ; i++) {
//        servers[i].join();
//    }
//    String msg = "Hello";
//    for (int i = 0 ; i < 100000 ; i++) {
//        (allNodes[i % 4]).addTransaction((msg + i).getBytes(), 0);
//    }
//
//    for (int i = 0 ; i < nnodes ; i++) {
//        (allNodes[i]).serve();
//    }
//    String[] ret = new String[4];
//    Thread[] tasks = new Thread[4];
//    for (int i = 0 ; i < nnodes ; i++) {
//        int finalI = i;
//        tasks[i] = new Thread(()-> {
//            try {
//                ret[finalI] = new String((allNodes[finalI]).deliver(1).getData(0).getData().toByteArray());
//            } catch (InterruptedException e) {
//                logger.error("", e);
//            }
//        });
//        tasks[i].start();
//    }
////    Thread.sleep(10 * 1000);
////    for (int i = 0 ; i < nnodes ; i++) {
////        tasks[i].join();
////    }
//    for (int i = 0 ; i < 4 ; i++) {
//        ( allNodes[i]).shutdown();
//    }
////        for (int i = 0 ; i < 4 ; i++) {
////            assertEquals(msg, ret[i]);
//////            ((RmfNode) allNodes[i]).stop();
////        }
//
//}
//    //

    void serverDoing(List<Block> res, cbcServer s) throws InterruptedException {
        logger.info("--->" + s.getID() + " STARTS");
        for(int i = 0; i < 100 ; i++) {
            String msg = "Hello" + i;
            if (i % 4 == s.getID()) {
                s.addTransaction(msg.getBytes(), 0);
            }
            res.add(s.deliver(i));
        }
        logger.info("--->" + s.getID() + " ENDS");
    }
    @Test
    void TestStressFourServersNoFailures() throws InterruptedException {
        setConfig(null, 0);
        Thread.sleep(timeToWaitBetweenTests);
        int nnodes = 4;
        logger.info("start TestFourServersNoFailures");
        Thread[] servers = new Thread[4];
        Node[] allNodes = initLocalBCNodes(nnodes,1, fourBbc.toString(),
                fourpanic.toString(), foursync.toString(), null);
        for (int i = 0 ; i < nnodes ; i++) {
            int finalI = i;
            servers[i]  = new Thread(() ->((cbcServer) allNodes[finalI]).start(false));
            servers[i].start();
        }
        for (int i = 0 ; i < nnodes ; i++) {
            servers[i].join();
        }
        for (int i = 0 ; i < nnodes ; i++) {
            ((cbcServer) allNodes[i]).serve();
        }
        Thread.sleep(5*1000);
        HashMap<Integer, ArrayList<Block>> res = new HashMap<>();
        for (int i = 0 ; i < nnodes ; i++) {
            res.put(i, new ArrayList<Block>());
        }
        logger.info("start TestStressFourServersNoFailures");
        Thread[] tasks = new Thread[4];
        for (int i = 0 ; i < 4 ;i++) {
            int finalI = i;
            tasks[i] = new Thread(() -> {
                try {
                    serverDoing(res.get(finalI), (cbcServer) allNodes[finalI]);
                } catch (InterruptedException e) {
                    logger.error("", e);
                }
            });
            tasks[i].start();
        }
        for (int i = 0 ; i < nnodes ; i++) {
            tasks[i].join();
        }
        for (int i = 0 ; i < 4 ; i++) {
            ((cbcServer) allNodes[i]).shutdown(false);
        }
        for (int i = 0 ; i < 100 ; i++) {
            logger.info(format("***** Assert creator = %d", res.get(0).get(i).getHeader().getM().getSender()));
            for (int k = 0 ; k < nnodes ;k++) {
                assertEquals(res.get(0).get(i).getHeader().getM().getSender(), res.get(k).get(i).getHeader().getM().getSender());
            }
        }

    }
//
@Test
void TestStressFourServersMuteFault() throws InterruptedException {
    setConfig(null, 0);
    Thread.sleep(timeToWaitBetweenTests);
    int nnodes = 4;
    logger.info("start TestFourServersNoFailures");
    Thread[] servers = new Thread[4];
    Node[] allNodes = initLocalBCNodes(nnodes,1, fourBbc.toString(),
            fourpanic.toString(), foursync.toString(), null);
    for (int i = 0 ; i < nnodes ; i++) {
        int finalI = i;
        servers[i]  = new Thread(() ->((cbcServer) allNodes[finalI]).start(false));
        servers[i].start();
    }
    for (int i = 0 ; i < nnodes ; i++) {
        servers[i].join();
    }

    for (int i = 1 ; i < nnodes ; i++) {
        ((cbcServer) allNodes[i]).serve();
    }

    HashMap<Integer, ArrayList<Block>> res = new HashMap<>();
    for (int i = 1 ; i < nnodes ; i++) {
        res.put(i, new ArrayList<Block>());
    }
    Thread[] tasks = new Thread[4];
    for (int i = 1 ; i < 4 ;i++) {
        int finalI = i;
        tasks[i] = new Thread(() -> {
            try {
                serverDoing(res.get(finalI), (cbcServer) allNodes[finalI]);
            } catch (InterruptedException e) {
                logger.error("", e);
            }
        });
        tasks[i].start();
    }
    for (int i = 1 ; i < nnodes ; i++) {
        tasks[i].join();
    }

    for (int i = 0 ; i < 4 ; i++) {
        ((cbcServer) allNodes[i]).shutdown(false);
    }

    for (int i = 0 ; i < 100 ; i++) {
        logger.info(format("***** Assert creator = %d", res.get(1).get(i).getHeader().getM().getSender()));
        for (int k = 1 ; k < nnodes ;k++) {
            assertEquals(res.get(1).get(i).getHeader().getM().getSender(), res.get(k).get(i).getHeader().getM().getSender());
        }
    }

}
    void bServerDoing(List<Block> res, byzantineBcServer s) throws InterruptedException {
        for(int i = 0; i < 100 ; i++) {
            String msg = "Hello" + i;
            if (i % 4 == s.getID()) {
                s.addTransaction(msg.getBytes(), 0);
            }
            logger.info(format("**** %d ***** %d", s.getID(), i));
            res.add(s.deliver(i));
        }
    }
    @Test
    void TestStressFourServersSelectiveBroadcastFault() throws InterruptedException {
        setConfig(fourConfig, 0);
        Thread.sleep(timeToWaitBetweenTests);
        int nnodes = 4;
        logger.info("start TestFourServersNoFailures");
        Thread[] servers = new Thread[4];
        Node[] allNodes = initLocalBCNodes(nnodes,1, fourBbc.toString(),
                fourpanic.toString(), foursync.toString(), new int[]{0});
        for (int i = 0 ; i < nnodes ; i++) {
            int finalI = i;
            if (i == 0) {
                servers[i]  = new Thread(() ->((byzantineBcServer) allNodes[finalI]).start(false));
                servers[i].start();
                continue;
            }
            servers[i]  = new Thread(() ->((cbcServer) allNodes[finalI]).start(false));
            servers[i].start();
        }
        for (int i = 0 ; i < nnodes ; i++) {
            servers[i].join();
        }

        for (int i = 0 ; i < nnodes ; i++) {
            if (i == 0) {
                ((byzantineBcServer) allNodes[i]).serve();
                continue;
            }
            ((cbcServer) allNodes[i]).serve();
        }

        HashMap<Integer, ArrayList<Block>> res = new HashMap<>();
        for (int i = 0 ; i < nnodes ; i++) {
            res.put(i, new ArrayList<Block>());
        }
        Thread[] tasks = new Thread[4];
        for (int i = 0 ; i < 4 ;i++) {
            if (i == 0) {
                int finalI1 = i;
                tasks[i] = new Thread(() -> {
                    try {
                        bServerDoing(res.get(finalI1), (byzantineBcServer) allNodes[finalI1]);
                    } catch (InterruptedException e) {
                        logger.error("", e);
                    }
                });
                tasks[i].start();
                continue;
            }
            int finalI = i;
            tasks[i] = new Thread(() -> {
                try {
                    serverDoing(res.get(finalI), (cbcServer) allNodes[finalI]);
                } catch (InterruptedException e) {
                    logger.error("", e);
                }
            });
            tasks[i].start();
        }
        for (int i = 0 ; i < nnodes ; i++) {
            tasks[i].join();
        }

        for (int i = 0 ; i < 4 ; i++) {
            if (i == 0) {
                ((byzantineBcServer) allNodes[i]).shutdown(false);
                continue;
            }
            ((cbcServer) allNodes[i]).shutdown(false);
        }

        for (int i = 0 ; i < 100 ; i++) {
            logger.info(format("***** Assert creator = %d", res.get(0).get(i).getHeader().getM().getSender()));
            for (int k = 0 ; k < nnodes ;k++) {
                assertEquals(res.get(0).get(i).getHeader().getM().getSender(), res.get(k).get(i).getHeader().getM().getSender());
            }
        }

    }

    void asyncServerDoing(List<Block> res, asyncBcServer s) throws InterruptedException {
        for(int i = 0; i < 100 ; i++) {
            String msg = "Hello" + i;
            if (i % 4 == s.getID()) {
                s.addTransaction(msg.getBytes(), 0);
            }
            res.add(s.deliver(i));
            logger.info(format("**** %d ***** %d", s.getID(), i));
        }
//        logger.info("******************" + s.getID() + "****************************");
    }
    @Test
    void TestStressFourServersAsyncNetwork() throws InterruptedException {
        setConfig(fourConfig, 0);
        Thread.sleep(timeToWaitBetweenTests);
        int nnodes = 4;
        logger.info("start TestFourServersNoFailures");
        Thread[] servers = new Thread[4];
        Node[] allNodes = initLocalAsyncBCNodes(nnodes,1, fourBbc.toString(),
                fourpanic.toString(), foursync.toString(), null);

        for (int i = 0 ; i < nnodes ; i++) {
            int finalI = i;
            servers[i]  = new Thread(() ->((asyncBcServer) allNodes[finalI]).start(false));
            servers[i].start();
        }
        for (int i = 0 ; i < nnodes ; i++) {
            servers[i].join();
        }

        for (int i = 0 ; i < nnodes ; i++) {
            ((asyncBcServer) allNodes[i]).setAsyncParam(1000);
            ((asyncBcServer) allNodes[i]).serve();
        }
        HashMap<Integer, ArrayList<Block>> res = new HashMap<>();
        for (int i = 0 ; i < nnodes ; i++) {
            res.put(i, new ArrayList<Block>());
        }
        Thread[] tasks = new Thread[4];
        for (int i = 0 ; i < 4 ;i++) {
            int finalI = i;
            tasks[i] = new Thread(() -> {
                try {
                    asyncServerDoing(res.get(finalI), (asyncBcServer) allNodes[finalI]);
                } catch (InterruptedException e) {
                    logger.error("", e);
                }
            });
            tasks[i].start();
        }


        for (int i = 0 ; i < nnodes ; i++) {
            tasks[i].join();
        }

        for (int i = 0 ; i < 100 ; i++) {
            logger.info(format("***** Assert creator = %d", res.get(0).get(i).getHeader().getM().getSender()));
            for (int k = 0 ; k < nnodes ;k++) {
                assertEquals(res.get(0).get(i).getHeader().getM().getSender(), res.get(k).get(i).getHeader().getM().getSender());
            }
        }

        for (int i = 0 ; i < 4 ; i++) {
            ((asyncBcServer) allNodes[i]).shutdown(false);
        }



    }

    @Test
    void TestFourServersSplitBroadcastFault() throws InterruptedException {
        setConfig(fourConfig, 0);
        Thread.sleep(timeToWaitBetweenTests);
        int nnodes = 4;
        logger.info("start TestFourServersNoFailures");
        Thread[] servers = new Thread[4];
        Node[] allNodes = initLocalBCNodes(nnodes,1, fourBbc.toString(),
                fourpanic.toString(), foursync.toString(), new int[]{0});
        for (int i = 0 ; i < nnodes ; i++) {
            int finalI = i;
            if (i == 0) {
                servers[i]  = new Thread(() ->((byzantineBcServer) allNodes[finalI]).start(false));
                servers[i].start();
                continue;
            }
            servers[i]  = new Thread(() ->((cbcServer) allNodes[finalI]).start(false));
            servers[i].start();
        }
        for (int i = 0 ; i < nnodes ; i++) {
            servers[i].join();
        }

        for (int i = 0 ; i < nnodes ; i++) {
            if (i == 0) {
                List<List<Integer>> gr = new ArrayList<>();
                gr.add(Arrays.asList(1, 2));
                gr.add(Arrays.asList(0 , 3));
                ((byzantineBcServer) allNodes[i]).setByzSetting(true, gr);
                ((byzantineBcServer) allNodes[i]).serve();
                continue;
            }
            ((cbcServer) allNodes[i]).serve();
        }

        Thread.sleep(10 * 60 * 1000);
        for (int i = 0 ; i < 4 ; i++) {
            if (i == 0) {
                ((byzantineBcServer) allNodes[i]).shutdown(false);
                continue;
            }
            ((cbcServer) allNodes[i]).shutdown(false);
        }
    }

    @Test
    void TestStressFourServersSplitBrodacastFault() throws InterruptedException {
        setConfig(fourConfig, 0);
        Thread.sleep(timeToWaitBetweenTests);
        int nnodes = 4;
        logger.info("start TestFourServersNoFailures");
        Thread[] servers = new Thread[4];
        Node[] allNodes = initLocalBCNodes(nnodes,1, fourBbc.toString(),
                fourpanic.toString(), foursync.toString(), new int[]{0});
        for (int i = 0 ; i < nnodes ; i++) {
            int finalI = i;
            if (i == 0) {
                servers[i]  = new Thread(() ->((byzantineBcServer) allNodes[finalI]).start(false));
                servers[i].start();
                continue;
            }
            servers[i]  = new Thread(() ->((cbcServer) allNodes[finalI]).start(false));
            servers[i].start();
        }
        for (int i = 0 ; i < nnodes ; i++) {
            servers[i].join();
        }

        for (int i = 0 ; i < nnodes ; i++) {
            if (i == 0) {
                List<List<Integer>> gr = new ArrayList<>();
                gr.add(Arrays.asList(1, 2));
                gr.add(Arrays.asList(0 , 3));
                ((byzantineBcServer) allNodes[i]).setByzSetting(true, gr);
                ((byzantineBcServer) allNodes[i]).serve();
                continue;
            }
            ((cbcServer) allNodes[i]).serve();
        }

        HashMap<Integer, ArrayList<Block>> res = new HashMap<>();
        for (int i = 0 ; i < nnodes ; i++) {
            res.put(i, new ArrayList<Block>());
        }
        Thread[] tasks = new Thread[4];
        for (int i = 0 ; i < 4 ;i++) {
            if (i == 0) {
                int finalI1 = i;
                tasks[i] = new Thread(() -> {
                    try {
                        bServerDoing(res.get(finalI1), (byzantineBcServer) allNodes[finalI1]);
                    } catch (InterruptedException e) {
                        logger.error("", e);
                    }
                });
                tasks[i].start();
                continue;
            }
            int finalI = i;
            tasks[i] = new Thread(() -> {
                try {
                    serverDoing(res.get(finalI), (cbcServer) allNodes[finalI]);
                } catch (InterruptedException e) {
                    logger.error("",e);
                }
            });
            tasks[i].start();
        }
        for (int i = 0 ; i < nnodes ; i++) {
            tasks[i].join();
        }

        for (int i = 0 ; i < 4 ; i++) {
            if (i == 0) {
                ((byzantineBcServer) allNodes[i]).shutdown(false);
                continue;
            }
            ((cbcServer) allNodes[i]).shutdown(false);
        }

        for (int i = 0 ; i < 100 ; i++) {
            logger.info(format("***** Assert creator = %d", res.get(0).get(i).getHeader().getM().getSender()));
            for (int k = 0 ; k < nnodes ;k++) {
                assertEquals(res.get(0).get(i).getHeader().getM().getSender(), res.get(k).get(i).getHeader().getM().getSender());
                for (int j = 0 ; j < res.get(0).get(i).getDataList().size() ; j++) {
                    logger.info(format("***** Assert data = %s",
                            new String(res.get(0).get(i).getData(j).getData().toByteArray())));
                    assertEquals(new String(res.get(0).get(i).getData(j).getData().toByteArray()),
                            new String(res.get(k).get(i).getData(j).getData().toByteArray()));
                }

            }
        }



    }

    @Test
    void TestStressAsyncFourServersSplitBrodacastFault() throws InterruptedException {
        setConfig(null, 0);
        Thread.sleep(timeToWaitBetweenTests);
        int nnodes = 4;
        logger.info("start TestFourServersNoFailures");
        Thread[] servers = new Thread[4];
        Node[] allNodes = initLocalAsyncBCNodes(nnodes,1, fourBbc.toString(),
                fourpanic.toString(), foursync.toString(), new int[]{0});
        for (int i = 0 ; i < nnodes ; i++) {
            int finalI = i;
            if (i == 0) {
                servers[i]  = new Thread(() ->((byzantineBcServer) allNodes[finalI]).start(false));
                servers[i].start();
                continue;
            }
            servers[i]  = new Thread(() ->((asyncBcServer) allNodes[finalI]).start(false));
            servers[i].start();
        }
        for (int i = 0 ; i < nnodes ; i++) {
            servers[i].join();
        }

        for (int i = 0 ; i < nnodes ; i++) {
            if (i == 0) {
                List<List<Integer>> gr = new ArrayList<>();
                gr.add(Arrays.asList(1, 2));
                gr.add(Arrays.asList(0 , 3));
                ((byzantineBcServer) allNodes[i]).setByzSetting(true, gr);
                ((byzantineBcServer) allNodes[i]).serve();
                continue;
            }
            ((asyncBcServer) allNodes[i]).setAsyncParam(1500);
            ((asyncBcServer) allNodes[i]).serve();
        }

        HashMap<Integer, ArrayList<Block>> res = new HashMap<>();
        for (int i = 0 ; i < nnodes ; i++) {
            res.put(i, new ArrayList<Block>());
        }
        Thread[] tasks = new Thread[4];
        for (int i = 0 ; i < 4 ;i++) {
            if (i == 0) {
                int finalI1 = i;
                tasks[i] = new Thread(() -> {
                    try {
                        bServerDoing(res.get(finalI1), (byzantineBcServer) allNodes[finalI1]);
                    } catch (InterruptedException e) {
                        logger.error("", e);
                    }
                });
                tasks[i].start();
                continue;
            }
            int finalI = i;
            tasks[i] = new Thread(() -> {
                try {
                    asyncServerDoing(res.get(finalI), (asyncBcServer) allNodes[finalI]);
                } catch (InterruptedException e) {
                    logger.error("", e);
                }
            });
            tasks[i].start();
        }
        for (int i = 0 ; i < nnodes ; i++) {
            tasks[i].join();
        }

        for (int i = 0 ; i < 4 ; i++) {
            if (i == 0) {
                ((byzantineBcServer) allNodes[i]).shutdown(false);
                continue;
            }
            ((asyncBcServer) allNodes[i]).shutdown(false);
        }

        for (int i = 0 ; i < 100 ; i++) {
            logger.info(format("***** Assert creator = %d", res.get(0).get(i).getHeader().getM().getSender()));
            for (int k = 0 ; k < nnodes ;k++) {
                assertEquals(res.get(0).get(i).getHeader().getM().getSender(), res.get(k).get(i).getHeader().getM().getSender());
                for (int j = 0 ; j < res.get(0).get(i).getDataList().size() ; j++) {
                    logger.info(format("***** Assert data = %s",
                            new String(res.get(0).get(i).getData(j).getData().toByteArray())));
                    assertEquals(new String(res.get(0).get(i).getData(j).getData().toByteArray()),
                            new String(res.get(k).get(i).getData(j).getData().toByteArray()));
                }

            }
        }

    }
}
