import blockchain.bcServer;
import com.google.protobuf.ByteString;
import config.Config;
import config.Node;
import org.apache.commons.lang.ArrayUtils;
import org.junit.jupiter.api.Test;
import proto.Block;
import proto.Transaction;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Random;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class blockchainTest {

    private int timeToWaitBetweenTests = 1; //1000 * 15;
    static Config conf = new Config();
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(blockchainTest.class);
    private String localHost = "127.0.0.1";
    private int[] ports = {20000, 20010, 20020, 20030};
    private Node s0= new Node(localHost, ports[0], 0);
    private Node s1= new Node(localHost, ports[1], 1);
    private Node s2= new Node(localHost, ports[2], 2);
    private Node s3= new Node(localHost, ports[3], 3);
    private ArrayList<Node> nodes = new ArrayList<Node>() {{add(s0); add(s1); add(s2); add(s3);}};

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


    private Node[] initLocalRmfNodes(int nnodes, int f, String configHome, int[] byzIds) {
        deleteViewIfExist(configHome);
        ArrayList<Node> currentNodes = new ArrayList<>(nodes.subList(0, nnodes));
        Node[] ret = new Node[nnodes];
        for (int id = 0 ; id < nnodes ; id++) {
            logger.info("init server #" + id);
//            if (ArrayUtils.contains(byzIds, id)) {
//                ret[id] = new ByzantineRmfNode(id, localHost, ports[id], f, 1, 1000 * 1, currentNodes, configHome);
//                continue;
//            }
            ret[id] =  new bcServer(localHost, ports[id], id, f, currentNodes, configHome, 1);
        }
        return ret;
        //        n.start();

    }
    @Test
    void TestSingleServer() throws InterruptedException {
        Thread.sleep(timeToWaitBetweenTests);
        logger.info("start TestSingleServer");
        String SingleServerconfigHome = Paths.get("config", "bbcConfig", "bbcSingleServer").toString();
        Node[] rn1 = initLocalRmfNodes(1, 0, SingleServerconfigHome, null);
        ((bcServer) rn1[0]).start();
        ((bcServer) rn1[0]).shutdown();
    }

    @Test
    void TestSingleServerDisseminateMessage() throws InterruptedException {
        Thread.sleep(timeToWaitBetweenTests);
        logger.info("start TestSingleServer");
        String SingleServerconfigHome = Paths.get("config", "bbcConfig", "bbcSingleServer").toString();
        Node[] rn1 = initLocalRmfNodes(1, 0, SingleServerconfigHome, null);
        ((bcServer) rn1[0]).start();
        ((bcServer) rn1[0]).addTransaction(("hello").getBytes(), 100);
        Block b = ((bcServer) rn1[0]).deliver();
        assertEquals(1, b.getHeader().getHeight());
        assertEquals(0, b.getHeader().getCreatorID());
        assertEquals(100, b.getData(0).getClientID());
        assertEquals("hello", new String(b.getData(0).getData().toByteArray()));
        ((bcServer) rn1[0]).shutdown();

    }

    @Test
    void TestFourServersNoFailuresSingleMessage() throws InterruptedException {
        Thread.sleep(timeToWaitBetweenTests);
        int nnodes = 4;
        String fourServersConfig = Paths.get("config", "bbcConfig", "bbcFourServers").toString();
        logger.info("start TestFourServersNoFailures");
        Thread[] servers = new Thread[4];
        Node[] allNodes = initLocalRmfNodes(nnodes,1,fourServersConfig, null);
        for (int i = 0 ; i < nnodes ; i++) {
            int finalI = i;
            servers[i]  = new Thread(() ->((bcServer) allNodes[finalI]).start());
            servers[i].start();
        }
        for (int i = 0 ; i < nnodes ; i++) {
            servers[i].join();
        }
//        try {
//        Thread.sleep(20 * 1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        int height = 0;
        String msg = "Hello";
        ((bcServer) allNodes[0]).addTransaction(msg.getBytes(), 0);
//        sender.act((RmfNode) allNodes[0]);
        String[] ret = new String[4];
        Thread[] tasks = new Thread[4];
        for (int i = 0 ; i < nnodes ; i++) {
            int finalI = i;
            tasks[i] = new Thread(()-> {
                ret[finalI] = new String(((bcServer) allNodes[finalI]).deliver().getData(0).toByteArray());
            });
            tasks[i].start();
        }

        for (int i = 0 ; i < nnodes ; i++) {
            tasks[i].join();
        }

        for (int i = 0 ; i < 4 ; i++) {
            assertEquals(msg, ret[i]);
//            ((RmfNode) allNodes[i]).stop();
        }
        for (int i = 0 ; i < 4 ; i++) {
            ((bcServer) allNodes[i]).shutdown();
        }
    }
//
//    @Test
//    void TestStressFourServersNoFailures() throws InterruptedException {
//        Thread.sleep(timeToWaitBetweenTests);
//        Thread[] servers = new Thread[4];
//        int nnodes = 4;
//        String fourServersConfig = Paths.get("config", "bbcConfig", "bbcFourServers").toString();
//        logger.info("start TestStressFourServersNoFailures");
//
//        Node[] allNodes = initLocalRmfNodes(nnodes,1,fourServersConfig, null);
//        for (int i = 0 ; i < nnodes ; i++) {
//            int finalI = i;
//            servers[i]  = new Thread(() ->((RmfNode) allNodes[finalI]).start());
//            servers[i].start();
//        }
//        for (int i = 0 ; i < nnodes ; i++) {
//            servers[i].join();
//        }
//        for (int k = 0 ; k < 100 ; k++) {
//            String msg = "Hello" + k;
//            ((RmfNode) allNodes[k % 4]).broadcast(msg.getBytes(), k);
////        sender.act((RmfNode) allNodes[0]);
//            String[] ret = new String[4];
//            Thread[] tasks = new Thread[4];
//            for (int i = 0 ; i < nnodes ; i++) {
//                int finalI = i;
//                int finalK = k;
//                tasks[i] = new Thread(()-> {
//                    ret[finalI] = new String(((RmfNode) allNodes[finalI]).deliver(finalK, finalK % 4));
//                });
//                tasks[i].start();
//            }
//
//            for (int i = 0 ; i < nnodes ; i++) {
//                tasks[i].join();
//            }
//
//            for (int i = 0 ; i < 4 ; i++) {
////                logger.info(format("******** round [%d:%d], message is [%s:%s] *********", k, i,  msg, ret[i]));
//                assertEquals(ret[0], ret[i]);
//            }
//        }
//
//        for (int i = 0 ; i < 4 ; i++) {
//            ((RmfNode) allNodes[i]).stop();
//        }
//    }
//
//    @Test
//    void TestFourServersSenderMuteFailure() throws InterruptedException {
//        Thread.sleep(timeToWaitBetweenTests);
//        Thread[] servers = new Thread[4];
//        int nnodes = 4;
//        String fourServersConfig = Paths.get("config", "bbcConfig", "bbcFourServers").toString();
//        logger.info("start TestFourServersSenderMuteFailure");
//
//        Node[] allNodes = initLocalRmfNodes(nnodes,1,fourServersConfig, null);
//        for (int i = 0 ; i < nnodes ; i++) {
//            int finalI = i;
//            servers[i]  = new Thread(() ->((RmfNode) allNodes[finalI]).start());
//            servers[i].start();
//        }
//        for (int i = 0 ; i < nnodes ; i++) {
//            servers[i].join();
//        }
//        int height = 0;
//        String[] ret = new String[4];
//        Thread[] tasks = new Thread[4];
//        for (int i = 0 ; i < nnodes ; i++) {
//            int finalI = i;
//            tasks[i] = new Thread(()-> {
//                byte[] res = ((RmfNode) allNodes[finalI]).deliver(height, 0);
//                ret[finalI] = (res == null ? null : new String(res));
//            });
//            tasks[i].start();
//        }
//
//        for (int i = 0 ; i < nnodes ; i++) {
//            tasks[i].join();
//        }
//
//        for (int i = 0 ; i < 4 ; i++) {
//            assertNull(ret[i]);
////            ((RmfNode) allNodes[i]).stop();
//        }
//        for (int i = 0 ; i < 4 ; i++) {
//            ((RmfNode) allNodes[i]).stop();
//        }
//    }
//    @Test
//    void TestFourServersSelectiveBroadcast() throws InterruptedException {
//        Thread.sleep(timeToWaitBetweenTests);
//        Thread[] servers = new Thread[4];
//        int nnodes = 4;
//        String fourServersConfig = Paths.get("config", "bbcConfig", "bbcFourServers").toString();
//        logger.info("start TestFourServersSelectiveBroadcast");
//
//        Node[] allNodes = initLocalRmfNodes(nnodes,1,fourServersConfig, new int[] {0});
//        for (int i = 0 ; i < nnodes ; i++) {
//            int finalI = i;
//            if (i == 0) {
//                servers[i] =  new Thread(() ->((ByzantineRmfNode) allNodes[finalI]).start());
//            } else {
//                servers[i]  = new Thread(() ->((RmfNode) allNodes[finalI]).start());
//            }
//            servers[i].start();
//        }
//        for (int i = 0 ; i < nnodes ; i++) {
//            servers[i].join();
//        }
//        int height = 0;
//        String msg = "Hello";
//        ((ByzantineRmfNode) allNodes[0]).selectiveBroadcast(msg.getBytes(), 0, new int[] {1, 2});
////        sender.act((RmfNode) allNodes[0]);
//        String[] ret = new String[4];
//        Thread[] tasks = new Thread[4];
//        for (int i = 0 ; i < nnodes ; i++) {
//            int finalI = i;
//            if (i == 0) {
//                tasks[i] = new Thread(()-> {
//                    byte[] res = ((ByzantineRmfNode) allNodes[finalI]).deliver(height, 0);
//                    ret[finalI] = (res == null ? null : new String(res));
//                });
//
//            } else {
//                tasks[i] = new Thread(()-> {
//                    byte[] res = ((RmfNode) allNodes[finalI]).deliver(height, 0);
//                    ret[finalI] = (res == null ? null : new String(res));
//                });
//            }
//
//            tasks[i].start();
//        }
//
//        for (int i = 0 ; i < nnodes ; i++) {
//            tasks[i].join();
//        }
//
//        for (int i = 0 ; i < 4 ; i++) {
//            assertEquals(ret[0], ret[i]);
////            ((RmfNode) allNodes[i]).stop();
//        }
//        for (int i = 0 ; i < 4 ; i++) {
//            if (i == 0)  {
//                ((ByzantineRmfNode) allNodes[i]).stop();
//                continue;
//            }
//            ((RmfNode) allNodes[i]).stop();
//        }
//
//    }
//
//    @Test
//    void TestStressFourServersSelectiveBroadcast() throws InterruptedException {
//        Thread.sleep(timeToWaitBetweenTests);
//        Thread[] servers = new Thread[4];
//        int nnodes = 4;
//        String fourServersConfig = Paths.get("config", "bbcConfig", "bbcFourServers").toString();
//        logger.info("start TestFourServersSelectiveBroadcast");
//
//        Node[] allNodes = initLocalRmfNodes(nnodes,1,fourServersConfig, new int[] {0});
//        for (int i = 0 ; i < nnodes ; i++) {
//            int finalI = i;
//            if (i == 0) {
//                servers[i] =  new Thread(() ->((ByzantineRmfNode) allNodes[finalI]).start());
//            } else {
//                servers[i]  = new Thread(() ->((RmfNode) allNodes[finalI]).start());
//            }
//            servers[i].start();
//        }
//        for (int i = 0 ; i < nnodes ; i++) {
//            servers[i].join();
//        }
//        for (int k = 0 ; k < 100 ; k++) {
//            String msg = "Hello" + k;
//            ((ByzantineRmfNode) allNodes[0]).selectiveBroadcast(msg.getBytes(), k, new int[] {1, 2});
////        sender.act((RmfNode) allNodes[0]);
//            String[] ret = new String[4];
//            Thread[] tasks = new Thread[4];
//            for (int i = 0 ; i < nnodes ; i++) {
//                int finalI = i;
//                if (i == 0) {
//                    int finalK = k;
//                    tasks[i] = new Thread(()-> {
//                        byte[] res = ((ByzantineRmfNode) allNodes[finalI]).deliver(finalK, 0);
//                        ret[finalI] = (res == null ? null : new String(res));
//                    });
//
//                } else {
//                    int finalK1 = k;
//                    tasks[i] = new Thread(()-> {
//                        byte[] res = ((RmfNode) allNodes[finalI]).deliver(finalK1, 0);
//                        ret[finalI] = (res == null ? null : new String(res));
//                    });
//                }
//
//                tasks[i].start();
//            }
//
//            for (int i = 0 ; i < nnodes ; i++) {
//                tasks[i].join();
//            }
//
//            for (int i = 0 ; i < 4 ; i++) {
//                assertEquals(ret[0], ret[i]);
////            ((RmfNode) allNodes[i]).stop();
//            }
//        }
//
//        for (int i = 0 ; i < 4 ; i++) {
//            if (i == 0)  {
//                ((ByzantineRmfNode) allNodes[i]).stop();
//                continue;
//            }
//            ((RmfNode) allNodes[i]).stop();
//        }
//
//    }
//
//    void asyncServerAction(RmfNode node, String[] res) throws InterruptedException {
//        for (int i = 0 ; i < 100 ; i++) {
//            Random rand = new Random();
//            int  n = rand.nextInt(1000);
//            if (n > 0) {
//                logger.info(format("[#%d] sleeps for [%d] ms", node.getID(), n));
//                Thread.sleep(n);
//            }
//            if (i % 4 == node.getID()) {
//                String msg = "hello" + i;
//                node.broadcast(msg.getBytes(), i);
//            }
//            byte[] ret = node.deliver(i, i % 4);
//            res[i] = (ret == null ? null : new String(ret));
//            if (i == 99) {
//                logger.info(format("[#%d] i=99", node.getID()));
//            }
//        }
//    }
//
//    @Test
//    void TestAsyncNetwork1() throws InterruptedException {
//        Thread.sleep(timeToWaitBetweenTests);
//        Thread[] servers = new Thread[4];
//        int nnodes = 4;
//        String fourServersConfig = Paths.get("config", "bbcConfig", "bbcFourServers").toString();
//        logger.info("start TestFourServersSenderMuteFailure");
//
//        Node[] allNodes = initLocalRmfNodes(nnodes,1,fourServersConfig, null);
//        for (int i = 0 ; i < nnodes ; i++) {
//            int finalI = i;
//            servers[i]  = new Thread(() ->((RmfNode) allNodes[finalI]).start());
//            servers[i].start();
//        }
//        for (int i = 0 ; i < nnodes ; i++) {
//            servers[i].join();
//        }
//        String[][] allRes = new String[4][100];
//        Thread[] tasks = new Thread[4];
//
//        for (int i = 0 ; i < 4; i++) {
//            int finalI = i;
//            tasks[i] = new Thread(() -> {
//                try {
//                    asyncServerAction((RmfNode) allNodes[finalI], allRes[finalI]);
//                } catch (InterruptedException e) {
//                    logger.warn("",e);
//                }
//            });
//            tasks[i].start();
//        }
//        for (int i = 0 ; i < 4 ;i++) {
//            tasks[i].join();
//        }
//
//        for (int i = 0 ; i < 100 ; i++) {
//            if (allRes[0][i] == null) {
//                logger.info(format("null message detected at round [%d]", i));
//            }
//            for (int j = 0 ; j < 4 ; j++) {
//
//                assertEquals(allRes[0][i], allRes[j][i]);
//            }
//        }
//
//        for (int i = 0 ; i < 4 ; i++) {
//            ((RmfNode) allNodes[i]).stop();
//            logger.info(format("[#%d] stopped", i));
//        }
//    }
}
