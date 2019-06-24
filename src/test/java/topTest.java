//import utils.Config;
//import utils.Node;
//import org.apache.commons.lang.ArrayUtils;
//import org.junit.jupiter.api.Test;
//import proto.Types;
//import servers.Top;
//
//import java.io.File;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//
//import static utils.Config.setConfig;
//import static java.lang.String.format;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//public class topTest {
//    private int timeToWaitBetweenTests = 1; //1000 * 60;
//    //    static Config conf = new Config();
////    Config.setConfig(null, 0);
//    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(topTest.class);
//    private String localHost = "127.0.0.1";
//    private int[] ports = {20000, 20010, 20020, 20030};
//    //    private int[] syncPort = {30000, 30010, 30020, 30030};
//    private ArrayList<Node> cluster = new ArrayList<Node>() {
//        {
//            add(new Node(localHost, ports[0], 0));
//            add(new Node(localHost, ports[1], 1));
//            add(new Node(localHost, ports[2], 2));
//            add(new Node(localHost, ports[3], 3));
//        }
//    };
////    private ArrayList<Node> c1 = new ArrayList<Node>() {
////        {
////            add(new Node(localHost, ports[0][1], 0));
////            add(new Node(localHost, ports[1][1], 1));
////            add(new Node(localHost, ports[2][1], 2));
////            add(new Node(localHost, ports[3][1], 3));
////        }
////    };
//
//    private Path singleBbc = Paths.get("Configurations", "single", "bbcConfig");
//    private Path singlepanic = Paths.get("Configurations", "single", "panicRBConfig");
//    private Path singlesync = Paths.get("Configurations", "single", "syncRBConfig");
//    private Path singleConfig = Paths.get("Configurations", "single", "config.toml");
//
//    private Path fourBbc = Paths.get("Configurations", "4Servers", "local", "bbcConfig");
//    private Path fourpanic = Paths.get("Configurations", "4Servers", "local", "panicRBConfig");
//    private Path foursync = Paths.get("Configurations", "4Servers", "local", "syncRBConfig");
//    private Path fourConfig = Paths.get("Configurations", "4Servers", "local", "config.toml");
//
//    private static void deleteViewIfExist(String configHome){
//        File file2 = new File(Paths.get(configHome, "currentView").toString());
//        if (file2.exists()) {
//            logger.info("Saved view found in " + file2.getAbsolutePath() + " deleting...");
//            if (file2.delete()) {
//                logger.info(file2.getName() + " has been deleted");
//            } else {
//                logger.warn(file2.getName() + " could not be deleted");
//            }
//        }
//    }
//
//
//    private Top[] initLocalSgNodes(int nnodes, int f, int g, String bbcConfig, String panicConfig, String syncConfig, int[] byzIds) {
//        deleteViewIfExist(bbcConfig);
//        deleteViewIfExist(panicConfig);
//        deleteViewIfExist(syncConfig);
////        ArrayList<ArrayList<Node>> clusters =  new ArrayList<>();
////        clusters.add(new ArrayList<>(c0.subList(0, nnodes)));
////        clusters.add(new ArrayList<>(c1.subList(0, nnodes)));
//        Top[] ret = new Top[nnodes];
//        for (int id = 0 ; id < nnodes ; id++) {
//            logger.info("init server #" + id);
//            if (ArrayUtils.contains(byzIds, id)) {
//                ret[id] = new Top(localHost, ports[id], id, f, g, 1000,
//                        100, 1, true, cluster, bbcConfig, panicConfig, syncConfig, "b",
//                        Config.getServerCrtPath(), Config.getServerTlsPrivKeyPath(), Config.getCaRootPath());
//                continue;
//            }
//            ret[id] = new Top(localHost, ports[id], id,f, g, 1000,
//                    100, 1, true, cluster, bbcConfig, panicConfig, syncConfig, "r",
//                    Config.getServerCrtPath(), Config.getServerTlsPrivKeyPath(), Config.getCaRootPath());
//        }
//        return ret;
//        //        n.start();
//
//    }
//
//    private Top[] initLocalAsyncBCNodes(int nnodes, int f, int g, String bbcConfig, String panicConfig, String syncConfig, int[] byzIds) {
//        deleteViewIfExist(bbcConfig);
//        deleteViewIfExist(panicConfig);
//        deleteViewIfExist(syncConfig);
////        ArrayList<ArrayList<Node>> clusters =  new ArrayList<>();
////        clusters.add((ArrayList<Node>) c0.subList(0, nnodes));
////        clusters.add((ArrayList<Node>) c1.subList(0, nnodes));
//        Top[] ret = new Top[nnodes];
//        for (int id = 0 ; id < nnodes ; id++) {
//            logger.info("init server #" + id);
//            if (ArrayUtils.contains(byzIds, id)) {
//                ret[id] = new Top(localHost, ports[id], id,f, g, 1000,
//                        100, 1, true, cluster, bbcConfig, panicConfig, syncConfig, "b",
//                        Config.getServerCrtPath(), Config.getServerTlsPrivKeyPath(), Config.getCaRootPath());
//                continue;
//            }
//            ret[id] = new Top(localHost, ports[id], id,f, g, 1000,
//                    100, 1, true, cluster, bbcConfig, panicConfig, syncConfig, "a",
//                    Config.getServerCrtPath(), Config.getServerTlsPrivKeyPath(), Config.getCaRootPath());
//        }
//        return ret;
//    }
//
//    @Test
//    void TestSingleServer() throws InterruptedException {
//        setConfig(singleConfig, 0);
//        Thread.sleep(timeToWaitBetweenTests);
//        logger.info("start TestSingleServer");
//        Top[] rn1 = initLocalSgNodes(1, 0, 2, singleBbc.toString(),
//                singlepanic.toString(), singlesync.toString(), null);
//        rn1[0].start();
//        rn1[0].shutdown();
//    }
//
//    @Test
//    void TestSingleServerDisseminateMessage() throws InterruptedException {
//        setConfig(singleConfig, 0);
//        Thread.sleep(timeToWaitBetweenTests);
//        logger.info("start TestSingleServer");
//        Top[] rn1 = initLocalSgNodes(1, 0, 2, singleBbc.toString(),
//                singlepanic.toString(), singlesync.toString(), null);
//        rn1[0].start();
//        rn1[0].serve();
////        for (int i = 0 ; i < 1000 ; i++) {
////            rn1[0].addTransaction(("hello").getBytes(), 100);
////        }
////        Thread.sleep(30 * 1000);
//        rn1[0].shutdown();
//
//    }
//
//    @Test
//    void TestFourServersNoFailuresSingleMessage() throws InterruptedException {
//        setConfig(fourConfig, 0);
//        Thread.sleep(timeToWaitBetweenTests);
//        int nnodes = 4;
//        logger.info("start TestFourServersNoFailures");
//        Thread[] servers = new Thread[4];
//        Top[] allNodes = initLocalSgNodes(nnodes,1, 2, fourBbc.toString(),
//                fourpanic.toString(), foursync.toString(), null);
//        for (int i = 0 ; i < nnodes ; i++) {
//            int finalI = i;
//            servers[i]  = new Thread(() -> {
//                (allNodes[finalI]).start();
//            });
//            servers[i].start();
//        }
//        for (int i = 0 ; i < nnodes ; i++) {
//            servers[i].join();
//        }
//        String msg = "Hello";
//        allNodes[0].addTransaction(msg.getBytes(), 0);
//        for (int i = 0 ; i < nnodes ; i++) {
//            allNodes[i].serve();
//        }
//        String[] ret = new String[4];
//        Thread[] tasks = new Thread[4];
//        for (int i = 0 ; i < nnodes ; i++) {
//            int finalI = i;
//            tasks[i] = new Thread(()-> {
//                try {
//                    ret[finalI] = new String(allNodes[finalI].deliver(1).getData(0).getData().toByteArray());
//                } catch (InterruptedException e) {
//                    logger.error("", e);
//                }
//            });
//            tasks[i].start();
//        }
//
//        for (int i = 0 ; i < nnodes ; i++) {
//            tasks[i].join();
//        }
//        for (int i = 0 ; i < 4 ; i++) {
//            allNodes[i].shutdown();
//        }
////        for (int i = 0 ; i < 4 ; i++) {
////            assertEquals(msg, ret[i]);
////            ((WrbNode) allNodes[i]).stop();
////        }
//
//    }
//
//    void serverDoing(List<Types.Block> res, Top s) throws InterruptedException {
//        logger.info("--->" + s.getID() + " STARTS");
//        for(int i = 0; i < 100 ; i++) {
//            String msg = "Hello" + i;
//            if (i % 4 == s.getID()) {
//                s.addTransaction(msg.getBytes(), 0);
//            }
//            res.add(s.deliver(i));
//        }
//        logger.info("--->" + s.getID() + " ENDS");
//    }
//    @Test
//    void TestStressFourServersNoFailures() throws InterruptedException {
//        setConfig(null, 0);
//        Thread.sleep(timeToWaitBetweenTests);
//        int nnodes = 4;
//        logger.info("start TestFourServersNoFailures");
//        Thread[] servers = new Thread[4];
//        Top[] allNodes = initLocalSgNodes(nnodes,1, 2, fourBbc.toString(),
//                fourpanic.toString(), foursync.toString(), null);
//        for (int i = 0 ; i < nnodes ; i++) {
//            int finalI = i;
//            servers[i]  = new Thread(() -> {
//                    allNodes[finalI].start();
//
//            });
//            servers[i].start();
//        }
//        for (int i = 0 ; i < nnodes ; i++) {
//            servers[i].join();
//        }
//        for (int i = 0 ; i < nnodes ; i++) {
//            allNodes[i].serve();
//        }
//        Thread.sleep(5*1000);
//        HashMap<Integer, ArrayList<Types.Block>> res = new HashMap<>();
//        for (int i = 0 ; i < nnodes ; i++) {
//            res.put(i, new ArrayList<Types.Block>());
//        }
//        logger.info("start TestStressFourServersNoFailures");
//        Thread[] tasks = new Thread[4];
//        for (int i = 0 ; i < 4 ;i++) {
//            int finalI = i;
//            tasks[i] = new Thread(() -> {
//                try {
//                    serverDoing(res.get(finalI), allNodes[finalI]);
//                } catch (InterruptedException e) {
//                    logger.error("", e);
//                }
//            });
//            tasks[i].start();
//        }
//        for (int i = 0 ; i < nnodes ; i++) {
//            tasks[i].join();
//        }
//        for (int i = 0 ; i < 4 ; i++) {
//            allNodes[i].shutdown();
//        }
//        for (int i = 0 ; i < 100 ; i++) {
//            logger.info(format("***** Assert creator = %d", res.get(0).get(i).getHeader().getM().getSender()));
//            for (int k = 0 ; k < nnodes ;k++) {
//                assertEquals(res.get(0).get(i).getHeader().getM().getSender(), res.get(k).get(i).getHeader().getM().getSender());
//            }
//        }
//
//    }
//
//    @Test
//    void TestStressFourServersMuteFault() throws InterruptedException {
//        setConfig(fourConfig, 0);
//        Thread.sleep(timeToWaitBetweenTests);
//        int nnodes = 4;
//        logger.info("start TestFourServersNoFailures");
//        Thread[] servers = new Thread[4];
//        Top[] allNodes = initLocalSgNodes(nnodes,1, 2, fourBbc.toString(),
//                fourpanic.toString(), foursync.toString(), null);
//        for (int i = 0 ; i < nnodes ; i++) {
//            int finalI = i;
//            servers[i]  = new Thread(() -> {
//                    allNodes[finalI].start();
//            });
//            servers[i].start();
//        }
//        for (int i = 0 ; i < nnodes ; i++) {
//            servers[i].join();
//        }
//
//        for (int i = 1 ; i < nnodes ; i++) {
//            allNodes[i].serve();
//        }
//
//        HashMap<Integer, ArrayList<Types.Block>> res = new HashMap<>();
//        for (int i = 1 ; i < nnodes ; i++) {
//            res.put(i, new ArrayList<Types.Block>());
//        }
//        Thread[] tasks = new Thread[4];
//        for (int i = 1 ; i < 4 ;i++) {
//            int finalI = i;
//            tasks[i] = new Thread(() -> {
//                try {
//                    serverDoing(res.get(finalI), allNodes[finalI]);
//                } catch (InterruptedException e) {
//                    logger.error("", e);
//                }
//            });
//            tasks[i].start();
//        }
//        for (int i = 1 ; i < nnodes ; i++) {
//            tasks[i].join();
//        }
//
//        for (int i = 0 ; i < 4 ; i++) {
//            allNodes[i].shutdown();
//        }
//
//        for (int i = 0 ; i < 100 ; i++) {
//            logger.info(format("***** Assert creator = %d", res.get(1).get(i).getHeader().getM().getSender()));
//            for (int k = 1 ; k < nnodes ;k++) {
//                assertEquals(res.get(1).get(i).getHeader().getM().getSender(), res.get(k).get(i).getHeader().getM().getSender());
//            }
//        }
//
//    }
//
//    void bServerDoing(List<Types.Block> res, Top s) throws InterruptedException {
//        for(int i = 0; i < 100 ; i++) {
//            String msg = "Hello" + i;
//            if (i % 4 == s.getID()) {
//                s.addTransaction(msg.getBytes(), 0);
//            }
//            logger.info(format("**** %d ***** %d", s.getID(), i));
//            res.add(s.deliver(i));
//        }
//    }
//    @Test
//    void TestStressFourServersSelectiveBroadcastFault() throws InterruptedException {
//        setConfig(fourConfig, 0);
//        Thread.sleep(timeToWaitBetweenTests);
//        int nnodes = 4;
//        logger.info("start TestFourServersNoFailures");
//        Thread[] servers = new Thread[4];
//        Top[] allNodes = initLocalSgNodes(nnodes,1, 2, fourBbc.toString(),
//                fourpanic.toString(), foursync.toString(), new int[]{0});
//        for (int i = 0 ; i < nnodes ; i++) {
//            int finalI = i;
//            if (i == 0) {
//                servers[i]  = new Thread(() -> {
//                        allNodes[finalI].start();
//                });
//                servers[i].start();
//                continue;
//            }
//            servers[i]  = new Thread(() -> {
//                    allNodes[finalI].start();
//            });
//            servers[i].start();
//        }
//        for (int i = 0 ; i < nnodes ; i++) {
//            servers[i].join();
//        }
//
//        for (int i = 0 ; i < nnodes ; i++) {
//            if (i == 0) {
//                List<List<Integer>> gr = new ArrayList<>();
//                gr.add(Arrays.asList(1, 2));
//                gr.add(Arrays.asList(0 , 3));
//                allNodes[i].setByzSetting(false, gr);
//                allNodes[i].serve();
//                continue;
//            }
//            allNodes[i].serve();
//        }
//
//        HashMap<Integer, ArrayList<Types.Block>> res = new HashMap<>();
//        for (int i = 0 ; i < nnodes ; i++) {
//            res.put(i, new ArrayList<Types.Block>());
//        }
//        Thread[] tasks = new Thread[4];
//        for (int i = 0 ; i < 4 ;i++) {
//            if (i == 0) {
//                int finalI1 = i;
//                tasks[i] = new Thread(() -> {
//                    try {
//                        bServerDoing(res.get(finalI1), allNodes[finalI1]);
//                    } catch (InterruptedException e) {
//                        logger.error("", e);
//                    }
//                });
//                tasks[i].start();
//                continue;
//            }
//            int finalI = i;
//            tasks[i] = new Thread(() -> {
//                try {
//                    serverDoing(res.get(finalI), allNodes[finalI]);
//                } catch (InterruptedException e) {
//                    logger.error("", e);
//                }
//            });
//            tasks[i].start();
//        }
//        for (int i = 0 ; i < nnodes ; i++) {
//            tasks[i].join();
//        }
//
//        for (int i = 0 ; i < 4 ; i++) {
//            if (i == 0) {
//                allNodes[i].shutdown();
//                continue;
//            }
//            allNodes[i].shutdown();
//        }
//
//        for (int i = 0 ; i < 100 ; i++) {
//            logger.info(format("***** Assert creator = %d", res.get(0).get(i).getHeader().getM().getSender()));
//            for (int k = 0 ; k < nnodes ;k++) {
//                assertEquals(res.get(0).get(i).getHeader().getM().getSender(), res.get(k).get(i).getHeader().getM().getSender());
//            }
//        }
//
//    }
//
//    void asyncServerDoing(List<Types.Block> res, Top s) throws InterruptedException {
//        if (s.getID() == 1) {
//            Thread.sleep(20 * 1000);
//        }
//        for(int i = 0; i < 100 ; i++) {
//            String msg = "Hello" + i;
//            if (i % 4 == s.getID()) {
//                s.addTransaction(msg.getBytes(), 0);
//            }
//            res.add(s.deliver(i));
//            logger.info(format("**** %d ***** %d", s.getID(), i));
//        }
////        logger.info("******************" + s.getID() + "****************************");
//    }
//
//    @Test
//    void TestStressFourServersAsyncNetwork() throws InterruptedException {
//        setConfig(fourConfig, 0);
//        Thread.sleep(timeToWaitBetweenTests);
//        int nnodes = 4;
//        logger.info("start TestFourServersNoFailures");
//        Thread[] servers = new Thread[4];
//        Top[] allNodes = initLocalAsyncBCNodes(nnodes,1, 2, fourBbc.toString(),
//                fourpanic.toString(), foursync.toString(), null);
//
//        for (int i = 0 ; i < nnodes ; i++) {
//            int finalI = i;
//            servers[i]  = new Thread(() -> {
//                    allNodes[finalI].start();
//            });
//            servers[i].start();
//        }
//        for (int i = 0 ; i < nnodes ; i++) {
//            servers[i].join();
//        }
//
//        for (int i = 0 ; i < nnodes ; i++) {
//            allNodes[i].setAsyncParam(1000);
//            allNodes[i].serve();
//        }
//        HashMap<Integer, ArrayList<Types.Block>> res = new HashMap<>();
//        for (int i = 0 ; i < nnodes ; i++) {
//            res.put(i, new ArrayList<Types.Block>());
//        }
//        Thread[] tasks = new Thread[4];
//        for (int i = 0 ; i < 4 ;i++) {
//            int finalI = i;
//            tasks[i] = new Thread(() -> {
//                try {
//                    asyncServerDoing(res.get(finalI), allNodes[finalI]);
//                } catch (InterruptedException e) {
//                    logger.error("", e);
//                }
//            });
//            tasks[i].start();
//        }
//
//
//        for (int i = 0 ; i < nnodes ; i++) {
//            tasks[i].join();
//        }
//
//        for (int i = 0 ; i < 100 ; i++) {
//            logger.info(format("***** Assert creator = %d", res.get(0).get(i).getHeader().getM().getSender()));
//            for (int k = 0 ; k < nnodes ;k++) {
//                assertEquals(res.get(0).get(i).getHeader().getM().getSender(), res.get(k).get(i).getHeader().getM().getSender());
//            }
//        }
//
//        for (int i = 0 ; i < 4 ; i++) {
//            allNodes[i].shutdown();
//        }
//    }
//
//    @Test
//    void TestStressFourServersSplitBrodacastFault() throws InterruptedException {
//        setConfig(fourConfig, 0);
//        Thread.sleep(timeToWaitBetweenTests);
//        int nnodes = 4;
//        logger.info("start TestFourServersNoFailures");
//        Thread[] servers = new Thread[4];
//        Top[] allNodes = initLocalSgNodes(nnodes,1, 2, fourBbc.toString(),
//                fourpanic.toString(), foursync.toString(), new int[]{0});
//        for (int i = 0 ; i < nnodes ; i++) {
//            int finalI = i;
//            if (i == 0) {
//                servers[i]  = new Thread(() -> {
//                        allNodes[finalI].start();
//                });
//                servers[i].start();
//                continue;
//            }
//            servers[i]  = new Thread(() -> {
//                    allNodes[finalI].start();
//            });
//            servers[i].start();
//        }
//        for (int i = 0 ; i < nnodes ; i++) {
//            servers[i].join();
//        }
//
//        for (int i = 0 ; i < nnodes ; i++) {
//            if (i == 0) {
//                List<List<Integer>> gr = new ArrayList<>();
//                gr.add(Arrays.asList(1, 2));
//                gr.add(Arrays.asList(0 , 3));
//                allNodes[i].setByzSetting(true, gr);
//                allNodes[i].serve();
//                continue;
//            }
//            allNodes[i].serve();
//        }
//
//        HashMap<Integer, ArrayList<Types.Block>> res = new HashMap<>();
//        for (int i = 0 ; i < nnodes ; i++) {
//            res.put(i, new ArrayList<Types.Block>());
//        }
//        Thread[] tasks = new Thread[4];
//        for (int i = 0 ; i < 4 ;i++) {
//            if (i == 0) {
//                int finalI1 = i;
//                tasks[i] = new Thread(() -> {
//                    try {
//                        bServerDoing(res.get(finalI1), allNodes[finalI1]);
//                    } catch (InterruptedException e) {
//                        logger.error("", e);
//                    }
//                });
//                tasks[i].start();
//                continue;
//            }
//            int finalI = i;
//            tasks[i] = new Thread(() -> {
//                try {
//                    serverDoing(res.get(finalI), allNodes[finalI]);
//                } catch (InterruptedException e) {
//                    logger.error("",e);
//                }
//            });
//            tasks[i].start();
//        }
//        for (int i = 0 ; i < nnodes ; i++) {
//            tasks[i].join();
//        }
//
//        for (int i = 0 ; i < 4 ; i++) {
//            if (i == 0) {
//                allNodes[i].shutdown();
//                continue;
//            }
//            allNodes[i].shutdown();
//        }
//
//        for (int i = 0 ; i < 100 ; i++) {
//            logger.info(format("***** Assert creator = %d", res.get(0).get(i).getHeader().getM().getSender()));
//            for (int k = 0 ; k < nnodes ;k++) {
//                assertEquals(res.get(0).get(i).getHeader().getM().getSender(), res.get(k).get(i).getHeader().getM().getSender());
//                for (int j = 0 ; j < res.get(0).get(i).getDataList().size() ; j++) {
//                    logger.info(format("***** Assert data = %s",
//                            new String(res.get(0).get(i).getData(j).getData().toByteArray())));
//                    assertEquals(new String(res.get(0).get(i).getData(j).getData().toByteArray()),
//                            new String(res.get(k).get(i).getData(j).getData().toByteArray()));
//                }
//
//            }
//        }
//    }
//
//    @Test
//    void TestStressAsyncFourServersSplitBrodacastFault() throws InterruptedException {
//        setConfig(null, 0);
//        Thread.sleep(timeToWaitBetweenTests);
//        int nnodes = 4;
//        logger.info("start TestFourServersNoFailures");
//        Thread[] servers = new Thread[4];
//        Top[] allNodes = initLocalAsyncBCNodes(nnodes,1, 2, fourBbc.toString(),
//                fourpanic.toString(), foursync.toString(), new int[]{0});
//        for (int i = 0 ; i < nnodes ; i++) {
//            int finalI = i;
//            servers[i]  = new Thread(() -> {
//                    allNodes[finalI].start();
//            });
//            servers[i].start();
//        }
//        for (int i = 0 ; i < nnodes ; i++) {
//            servers[i].join();
//        }
//
//        for (int i = 0 ; i < nnodes ; i++) {
//            if (i == 0) {
//                List<List<Integer>> gr = new ArrayList<>();
//                gr.add(Arrays.asList(1, 2));
//                gr.add(Arrays.asList(0 , 3));
//                allNodes[i].setByzSetting(true, gr);
//                allNodes[i].serve();
//                continue;
//            }
//            allNodes[i].setAsyncParam(2000);
//            allNodes[i].serve();
//        }
//
//        HashMap<Integer, ArrayList<Types.Block>> res = new HashMap<>();
//        for (int i = 0 ; i < nnodes ; i++) {
//            res.put(i, new ArrayList<Types.Block>());
//        }
//        Thread[] tasks = new Thread[4];
//        for (int i = 0 ; i < 4 ;i++) {
//            if (i == 0) {
//                int finalI1 = i;
//                tasks[i] = new Thread(() -> {
//                    try {
//                        bServerDoing(res.get(finalI1), allNodes[finalI1]);
//                    } catch (InterruptedException e) {
//                        logger.error("", e);
//                    }
//                });
//                tasks[i].start();
//                continue;
//            }
//            int finalI = i;
//            tasks[i] = new Thread(() -> {
//                try {
//                    asyncServerDoing(res.get(finalI),  allNodes[finalI]);
//                } catch (InterruptedException e) {
//                    logger.error("", e);
//                }
//            });
//            tasks[i].start();
//        }
//        for (int i = 0 ; i < nnodes ; i++) {
//            tasks[i].join();
//        }
//
//        for (int i = 0 ; i < 4 ; i++) {
//            if (i == 0) {
//                allNodes[i].shutdown();
//                continue;
//            }
//            allNodes[i].shutdown();
//        }
//
//        for (int i = 0 ; i < 100 ; i++) {
//            logger.info(format("***** Assert creator = %d", res.get(0).get(i).getHeader().getM().getSender()));
//            for (int k = 0 ; k < nnodes ;k++) {
//                assertEquals(res.get(0).get(i).getHeader().getM().getSender(), res.get(k).get(i).getHeader().getM().getSender());
//                for (int j = 0 ; j < res.get(0).get(i).getDataList().size() ; j++) {
//                    logger.info(format("***** Assert data = %s",
//                            new String(res.get(0).get(i).getData(j).getData().toByteArray())));
//                    assertEquals(new String(res.get(0).get(i).getData(j).getData().toByteArray()),
//                            new String(res.get(k).get(i).getData(j).getData().toByteArray()));
//                }
//
//            }
//        }
//
//    }
//
//}
