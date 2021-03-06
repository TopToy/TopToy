//import utils.config.Config;
//import utils.Node;
//import org.apache.commons.lang.ArrayUtils;
//import org.junit.jupiter.api.Test;
//import das.wrb.ByzantineWrbNode;
//import das.wrb.WrbNode;
//
//import java.io.File;
//import java.nio.file.Paths;
//import java.util.ArrayList;
//
//import static java.lang.String.format;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNull;
//
//public class rmfTest {
//    int cidSeires = 0;
//    private int timeToWaitBetweenTests = 1; //1000 * 15;
////    static Config conf = new Config();
//    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(rmfTest.class);
//    private String localHost = "localhost";
//    private int[] rmfPorts = {20000, 20010, 20020, 20030};
//    private Node s0= new Node(localHost, rmfPorts[0], 0);
//    private Node s1= new Node(localHost, rmfPorts[1], 1);
//    private Node s2= new Node(localHost, rmfPorts[2], 2);
//    private Node s3= new Node(localHost, rmfPorts[3], 3);
//    private ArrayList<Node> nodes = new ArrayList<Node>() {{add(s0); add(s1); add(s2); add(s3);}};
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
//    private Node[] initLocalRmfNodes(int nnodes, int f, String configHome, int[] byzIds) {
//        deleteViewIfExist(configHome);
//        ArrayList<Node> currentNodes = new ArrayList<>(nodes.subList(0, nnodes));
//        Node[] ret = new Node[nnodes];
//        for (int id = 0 ; id < nnodes ; id++) {
//            logger.info("init server #" + id);
//            if (ArrayUtils.contains(byzIds, id)) {
//                ret[id] = new ByzantineWrbNode(1, id, localHost, rmfPorts[id], f, 1000, 100, currentNodes, configHome,
//                        Config.getServerCrtPath(), Config.getServerTlsPrivKeyPath(), Config.getCaRootPath());
//                continue;
//            }
//            ret[id] =  new WrbNode(1, id, localHost, rmfPorts[id], f, 1000, 100, currentNodes, configHome,
//                    Config.getServerCrtPath(), Config.getServerTlsPrivKeyPath(), Config.getCaRootPath());
//        }
//        return ret;
//
//    }
//    @Test
//    void TestSingleServer() throws InterruptedException {
//        Thread.sleep(timeToWaitBetweenTests);
//        logger.info("start TestSingleServer");
//        String SingleServerconfigHome = Paths.get("config", "Configurations/single server/bbcConfig", "bbcSingleServer").toString();
//        Node[] rn1 = initLocalRmfNodes(1, 0, SingleServerconfigHome, null);
//        ((WrbNode) rn1[0]).start();
//        ((WrbNode) rn1[0]).stop();
//    }
//
////    @Test
////    void TestSingleServerDisseminateMessage() throws InterruptedException {
////        int cid = 0;
////        Thread.sleep(timeToWaitBetweenTests);
////        logger.info("start TestSingleServerDisseminateMessage");
////        String SingleServerconfigHome = Paths.get("config", "Configurations/single server/bbcConfig", "bbcSingleServer").toString();
////        Node[] rn = initLocalRmfNodes( 1, 0, SingleServerconfigHome, null);
////        ((WrbNode) rn[0]).start();
////        String msg = "hello world";
////        ((WrbNode) rn[0]).broadcast(0, cidSeires, cid, msg.getBytes(), 0);
////        assertEquals(msg, new String(((WrbNode) rn[0]).deliver(0, cidSeires, cid, 0, 0, 10 * 60 * 1000, null)[0]));
////        ((WrbNode) rn[0]).stop();
////
////    }
////
////    @Test
////    void TestFourServersNoFailuresSingleMessage() throws InterruptedException {
////        setConfig(Paths.get("Configurations", "4Servers", "local", "config.toml"), 0);
////        Thread.sleep(timeToWaitBetweenTests);
////        Thread[] servers = new Thread[4];
////        int nnodes = 4;
////        String fourServersConfig = Paths.get("Configurations", "4Servers", "local", "bbcConfig").toString();
////        logger.info("start TestFourServersNoFailures");
////
////        Node[] allNodes = initLocalRmfNodes(nnodes,1,fourServersConfig, null);
////        for (int i = 0 ; i < nnodes ; i++) {
////            int finalI = i;
////            servers[i]  = new Thread(() ->((WrbNode) allNodes[finalI]).start());
////            servers[i].start();
////        }
////        for (int i = 0 ; i < nnodes ; i++) {
////            servers[i].join();
////        }
////        int height = 0;
////        String msg = "Hello";
////        ((WrbNode) allNodes[0]).broadcast(0, 0, 0, msg.getBytes(), 0);
////        String[] ret = new String[4];
////        Thread[] tasks = new Thread[4];
////        for (int i = 0 ; i < nnodes ; i++) {
////            int finalI = i;
////            tasks[i] = new Thread(()-> {
////                try {
////                    ret[finalI] = new String(((WrbNode) allNodes[finalI]).deliver(0, 0, 0, height, 0, 1000, null)[0]);
////                } catch (InterruptedException e) {
////                    e.printStackTrace();
////                }
////            });
////            tasks[i].start();
////        }
////
////        for (int i = 0 ; i < nnodes ; i++) {
////            tasks[i].join();
////        }
////
////        for (int i = 0 ; i < 4 ; i++) {
////            assertEquals(msg, ret[i]);
////        }
////        for (int i = 0 ; i < 4 ; i++) {
////            ((WrbNode) allNodes[i]).stop();
////        }
////    }
////
////    @Test
////    void TestStressFourServersNoFailures() throws InterruptedException {
////        setConfig(Paths.get("Configurations", "4Servers", "local", "config.toml"), 0);
////        Thread.sleep(timeToWaitBetweenTests);
////        Thread[] servers = new Thread[4];
////        int nnodes = 4;
////        String fourServersConfig = Paths.get("Configurations", "4Servers", "local", "bbcConfig").toString();
////        logger.info("start TestStressFourServersNoFailures");
////
////        Node[] allNodes = initLocalRmfNodes(nnodes,1,fourServersConfig, null);
////        for (int i = 0 ; i < nnodes ; i++) {
////            int finalI = i;
////            servers[i]  = new Thread(() ->((WrbNode) allNodes[finalI]).start());
////            servers[i].start();
////        }
////        for (int i = 0 ; i < nnodes ; i++) {
////            servers[i].join();
////        }
////        for (int k = 0 ; k < 100 ; k++) {
////            String msg = "Hello" + k;
////            ((WrbNode) allNodes[k % 4]).broadcast(0, cidSeires, k, msg.getBytes(), k);
////            String[] ret = new String[4];
////            Thread[] tasks = new Thread[4];
////            for (int i = 0 ; i < nnodes ; i++) {
////                int finalI = i;
////                int finalK = k;
////                tasks[i] = new Thread(()-> {
////                    try {
////                        ret[finalI] = new String(((WrbNode) allNodes[finalI]).deliver(0, cidSeires, finalK, finalK,
////                                finalK % 4, 1* 1000, null)[0]);
////                    } catch (InterruptedException e) {
////                        e.printStackTrace();
////                    }
////                });
////                tasks[i].start();
////            }
////
////            for (int i = 0 ; i < nnodes ; i++) {
////                tasks[i].join();
////            }
////
////            for (int i = 0 ; i < 4 ; i++) {
////                assertEquals(ret[0], ret[i]);
////            }
////        }
////        for (int i = 0 ; i < 4 ; i++) {
////            ((WrbNode) allNodes[i]).stop();
////        }
////    }
////
////    @Test
////    void TestFourServersSenderMuteFailure() throws InterruptedException {
////        setConfig(Paths.get("Configurations", "4Servers", "local", "config.toml"), 0);
////        Thread.sleep(timeToWaitBetweenTests);
////        Thread[] servers = new Thread[4];
////        int nnodes = 4;
////        String fourServersConfig = Paths.get("Configurations", "4Servers", "local", "bbcConfig").toString();
////        logger.info("start TestFourServersSenderMuteFailure");
////
////        Node[] allNodes = initLocalRmfNodes(nnodes,1,fourServersConfig, null);
////        for (int i = 0 ; i < nnodes ; i++) {
////            int finalI = i;
////            servers[i]  = new Thread(() ->((WrbNode) allNodes[finalI]).start());
////            servers[i].start();
////        }
////        for (int i = 0 ; i < nnodes ; i++) {
////            servers[i].join();
////        }
////        int height = 0;
////        String[] ret = new String[4];
////        Thread[] tasks = new Thread[4];
////        for (int i = 0 ; i < nnodes ; i++) {
////            int finalI = i;
////            tasks[i] = new Thread(()-> {
////                byte[] res = new byte[0];
////                try {
////                    res = ((WrbNode) allNodes[finalI]).deliver(0, 0, 0, height, 0, 1000,
////                            null)[0];
////                } catch (InterruptedException e) {
////                    e.printStackTrace();
////                }
////                ret[finalI] = (res == null ? null : new String(res));
////            });
////            tasks[i].start();
////        }
////
////        for (int i = 0 ; i < nnodes ; i++) {
////            tasks[i].join();
////        }
////
////        for (int i = 0 ; i < 4 ; i++) {
////            assertNull(ret[i]);
////        }
////        for (int i = 0 ; i < 4 ; i++) {
////            ((WrbNode) allNodes[i]).stop();
////        }
////    }
////    @Test
////    void TestFourServersSelectiveBroadcast() throws InterruptedException {
////        setConfig(Paths.get("Configurations", "4Servers", "local", "config.toml"), 0);
////        Thread.sleep(timeToWaitBetweenTests);
////        Thread[] servers = new Thread[4];
////        int nnodes = 4;
////        String fourServersConfig = Paths.get("Configurations", "4Servers", "local", "bbcConfig").toString();
////        logger.info("start TestFourServersSelectiveBroadcast");
////
////        Node[] allNodes = initLocalRmfNodes(nnodes,1,fourServersConfig, new int[] {0});
////        for (int i = 0 ; i < nnodes ; i++) {
////            int finalI = i;
////            if (i == 0) {
////                servers[i] =  new Thread(() ->((ByzantineWrbNode) allNodes[finalI]).start());
////            } else {
////                servers[i]  = new Thread(() ->((WrbNode) allNodes[finalI]).start());
////            }
////            servers[i].start();
////        }
////        for (int i = 0 ; i < nnodes ; i++) {
////            servers[i].join();
////        }
////        int height = 0;
////        String msg = "Hello";
////        List<Integer> ids = new ArrayList<Integer>();
////        ids.add(1);
////        ids.add(2);
////        ((ByzantineWrbNode) allNodes[0]).selectiveBroadcast(0, 0, 0, msg.getBytes(), 0, ids);
////        String[] ret = new String[4];
////        Thread[] tasks = new Thread[4];
////        for (int i = 0 ; i < nnodes ; i++) {
////            int finalI = i;
////            if (i == 0) {
////                tasks[i] = new Thread(()-> {
////                    byte[] res = new byte[0];
////                    try {
////                        res = ((ByzantineWrbNode) allNodes[finalI]).deliver(0, 0, 0, height, 0, 1000,
////                                null)[0];
////                    } catch (InterruptedException e) {
////                        e.printStackTrace();
////                    }
////                    ret[finalI] = (res == null ? null : new String(res));
////                });
////
////            } else {
////                tasks[i] = new Thread(()-> {
////                    byte[] res = new byte[0];
////                    try {
////                        res = ((WrbNode) allNodes[finalI]).deliver(0, 0, 0, height, 0, 1000,
////                                null)[0];
////                    } catch (InterruptedException e) {
////                        e.printStackTrace();
////                    }
////                    ret[finalI] = (res == null ? null : new String(res));
////                });
////            }
////
////            tasks[i].start();
////        }
////
////        for (int i = 0 ; i < nnodes ; i++) {
////            tasks[i].join();
////        }
////
////        for (int i = 0 ; i < 4 ; i++) {
////            assertEquals(ret[0], ret[i]);
////        }
////        for (int i = 0 ; i < 4 ; i++) {
////            if (i == 0)  {
////                ((ByzantineWrbNode) allNodes[i]).stop();
////                continue;
////            }
////            ((WrbNode) allNodes[i]).stop();
////        }
////
////    }
////
////    @Test
////    void TestStressFourServersSelectiveBroadcast() throws InterruptedException {
////        setConfig(Paths.get("Configurations", "4Servers", "local", "config.toml"), 0);
////        Thread.sleep(timeToWaitBetweenTests);
////        Thread[] servers = new Thread[4];
////        int nnodes = 4;
////        String fourServersConfig = Paths.get("Configurations", "4Servers", "local", "bbcConfig").toString();
////        logger.info("start TestFourServersSelectiveBroadcast");
////
////        Node[] allNodes = initLocalRmfNodes(nnodes,1,fourServersConfig, new int[] {0});
////        for (int i = 0 ; i < nnodes ; i++) {
////            int finalI = i;
////            if (i == 0) {
////                servers[i] =  new Thread(() ->((ByzantineWrbNode) allNodes[finalI]).start());
////            } else {
////                servers[i]  = new Thread(() ->((WrbNode) allNodes[finalI]).start());
////            }
////            servers[i].start();
////        }
////        for (int i = 0 ; i < nnodes ; i++) {
////            servers[i].join();
////        }
////        List<Integer> ids = new ArrayList<Integer>();
////        ids.add(1);
////        ids.add(2);
////        for (int k = 0 ; k < 100 ; k++) {
////            String msg = "Hello" + k;
////            ((ByzantineWrbNode) allNodes[0]).selectiveBroadcast(0, 0, k, msg.getBytes(), k, ids);
//////        sender.act((WrbNode) allNodes[0]);
////            String[] ret = new String[4];
////            Thread[] tasks = new Thread[4];
////            for (int i = 0 ; i < nnodes ; i++) {
////                int finalI = i;
////                if (i == 0) {
////                    int finalK = k;
////                    tasks[i] = new Thread(()-> {
////                        byte[] res = new byte[0];
////                        try {
////                            res = ((ByzantineWrbNode) allNodes[finalI]).deliver(0, 0, finalK, finalK, 0, 1000,
////                                    null)[0];
////                        } catch (InterruptedException e) {
////                            e.printStackTrace();
////                        }
////                        ret[finalI] = (res == null ? null : new String(res));
////                    });
////
////                } else {
////                    int finalK1 = k;
////                    tasks[i] = new Thread(()-> {
////                        byte[] res = new byte[0];
////                        try {
////                            res = ((WrbNode) allNodes[finalI]).deliver(0, 0, finalK1, finalK1, 0, 1000,
////                                    null)[0];
////                        } catch (InterruptedException e) {
////                            e.printStackTrace();
////                        }
////                        ret[finalI] = (res == null ? null : new String(res));
////                    });
////                }
////
////                tasks[i].start();
////            }
////
////            for (int i = 0 ; i < nnodes ; i++) {
////                tasks[i].join();
////            }
////
////            for (int i = 0 ; i < 4 ; i++) {
////                assertEquals(ret[0], ret[i]);
////            }
////        }
////
////        for (int i = 0 ; i < 4 ; i++) {
////            if (i == 0)  {
////                ((ByzantineWrbNode) allNodes[i]).stop();
////                continue;
////            }
////            ((WrbNode) allNodes[i]).stop();
////        }
////
////    }
////
////    void asyncServerAction(WrbNode node, String[] res) throws InterruptedException {
////        for (int i = 0 ; i < 100 ; i++) {
////            Random rand = new Random();
////            int  n = rand.nextInt(800);
////            if (n > 0) {
////                logger.info(format("[#%d] sleeps for [%d] ms", node.getID(), n));
////                Thread.sleep(n);
////            }
////            if (i % 4 == node.getID()) {
////                String msg = "hello" + i;
////                node.broadcast(0, 0, i, msg.getBytes(), i);
////            }
////            byte[] ret = node.deliver(0, 0, i, i, i % 4, 1000,
////                    null)[0];
////            res[i] = (ret == null ? null : new String(ret));
////            if (i == 99) {
////                logger.info(format("[#%d] i=99", node.getID()));
////            }
////        }
////    }
////
////    @Test
////    void TestAsyncNetwork1() throws InterruptedException {
////        setConfig(Paths.get("Configurations", "4Servers", "local", "config.toml"), 0);
////        Thread.sleep(timeToWaitBetweenTests);
////        Thread[] servers = new Thread[4];
////        int nnodes = 4;
////        String fourServersConfig = Paths.get("Configurations", "4Servers", "local", "bbcConfig").toString();
////        logger.info("start TestFourServersSenderMuteFailure");
////
////        Node[] allNodes = initLocalRmfNodes(nnodes,1,fourServersConfig, null);
////        for (int i = 0 ; i < nnodes ; i++) {
////            int finalI = i;
////            servers[i]  = new Thread(() ->((WrbNode) allNodes[finalI]).start());
////            servers[i].start();
////        }
////        for (int i = 0 ; i < nnodes ; i++) {
////            servers[i].join();
////        }
////        String[][] allRes = new String[4][100];
////        Thread[] tasks = new Thread[4];
////
////        for (int i = 0 ; i < 4; i++) {
////            int finalI = i;
////            tasks[i] = new Thread(() -> {
////                try {
////                    asyncServerAction((WrbNode) allNodes[finalI], allRes[finalI]);
////                } catch (InterruptedException e) {
////                    logger.warn("",e);
////                }
////            });
////            tasks[i].start();
////        }
////        for (int i = 0 ; i < 4 ;i++) {
////            tasks[i].join();
////        }
////
////        for (int i = 0 ; i < 100 ; i++) {
////            if (allRes[0][i] == null) {
////                logger.info(format("null message detected at round [%d]", i));
////            }
////            for (int j = 0 ; j < 4 ; j++) {
////
////                assertEquals(allRes[0][i], allRes[j][i]);
////            }
////        }
////
////        for (int i = 0 ; i < 4 ; i++) {
////            ((WrbNode) allNodes[i]).stop();
////            logger.info(format("[#%d] stopped", i));
////        }
////    }
//
//}
