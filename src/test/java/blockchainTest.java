import blockchain.asyncBcServer;
import blockchain.bcServer;
import blockchain.byzantineBcServer;
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
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class blockchainTest {

    private int timeToWaitBetweenTests = 1000 * 60;
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
            if (ArrayUtils.contains(byzIds, id)) {
                ret[id] = new byzantineBcServer(localHost, ports[id], id, f, currentNodes, configHome, 1);
                continue;
            }
            ret[id] =  new bcServer(localHost, ports[id], id, f, currentNodes, configHome, 1);
        }
        return ret;
        //        n.start();

    }

    private Node[] initLocalAsyncRmfNodes(int nnodes, int f, String configHome, int[] byzIds) {
        deleteViewIfExist(configHome);
        ArrayList<Node> currentNodes = new ArrayList<>(nodes.subList(0, nnodes));
        Node[] ret = new Node[nnodes];
        for (int id = 0 ; id < nnodes ; id++) {
            logger.info("init server #" + id);
            ret[id] =  new asyncBcServer(localHost, ports[id], id, f, currentNodes, configHome, 1);
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
        ((bcServer) rn1[0]).serve();
        ((bcServer) rn1[0]).addTransaction(("hello").getBytes(), 100);
        Block b = ((bcServer) rn1[0]).deliver(1);
        assertEquals(1, b.getHeader().getHeight());
        assertEquals(0, b.getHeader().getCreatorID());
//        assertEquals(100, b.getData(0).getClientID());
//        assertEquals("hello", new String(b.getData(0).getData().toByteArray()));
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
        String msg = "Hello";
        ((bcServer) allNodes[0]).addTransaction(msg.getBytes(), 0);
        for (int i = 0 ; i < nnodes ; i++) {
            ((bcServer) allNodes[i]).serve();
        }
//        try {
//        Thread.sleep(20 * 1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        int height = 0;
//        sender.act((RmfNode) allNodes[0]);
//        Thread.sleep(10 * 1000);
        String[] ret = new String[4];
        Thread[] tasks = new Thread[4];
        for (int i = 0 ; i < nnodes ; i++) {
            int finalI = i;
            tasks[i] = new Thread(()-> {
                ret[finalI] = new String(((bcServer) allNodes[finalI]).deliver(1).getData(0).getData().toByteArray());
            });
            tasks[i].start();
        }

        for (int i = 0 ; i < nnodes ; i++) {
            tasks[i].join();
        }
        for (int i = 0 ; i < 4 ; i++) {
            ((bcServer) allNodes[i]).shutdown();
        }
        for (int i = 0 ; i < 4 ; i++) {
            assertEquals(msg, ret[i]);
//            ((RmfNode) allNodes[i]).stop();
        }

    }
//
    void serverDoing(List<Block> res, bcServer s) {
        for(int i = 0; i < 100 ; i++) {
            String msg = "Hello" + i;
            if (i % 4 == s.getID()) {
                s.addTransaction(msg.getBytes(), 0);
            }
            res.add(s.deliver(i));
        }
//        logger.info("******************" + s.getID() + "****************************");
    }
    @Test
    void TestStressFourServersNoFailures() throws InterruptedException {
        Thread.sleep(timeToWaitBetweenTests);
        Thread[] servers = new Thread[4];
        int nnodes = 4;
        String fourServersConfig = Paths.get("config", "bbcConfig", "bbcFourServers").toString();
        logger.info("start TestStressFourServersNoFailures");

        Node[] allNodes = initLocalRmfNodes(nnodes,1,fourServersConfig, null);
        for (int i = 0 ; i < nnodes ; i++) {
            int finalI = i;
            servers[i]  = new Thread(() ->((bcServer) allNodes[finalI]).start());
            servers[i].start();
        }
        for (int i = 0 ; i < nnodes ; i++) {
            servers[i].join();
        }

        for (int i = 0 ; i < nnodes ; i++) {
            ((bcServer) allNodes[i]).serve();
        }
        HashMap<Integer, ArrayList<Block>> res = new HashMap<>();
        for (int i = 0 ; i < nnodes ; i++) {
            res.put(i, new ArrayList<Block>());
        }
        Thread[] tasks = new Thread[4];
        for (int i = 0 ; i < 4 ;i++) {
            int finalI = i;
            tasks[i] = new Thread(() -> serverDoing(res.get(finalI), (bcServer) allNodes[finalI]));
            tasks[i].start();
        }
        for (int i = 0 ; i < nnodes ; i++) {
            tasks[i].join();
        }
        for (int i = 0 ; i < 4 ; i++) {
            ((bcServer) allNodes[i]).shutdown();
        }
        for (int i = 0 ; i < 100 ; i++) {
            logger.info(format("***** Assert creator = %d", res.get(0).get(i).getHeader().getCreatorID()));
            for (int k = 0 ; k < nnodes ;k++) {
                assertEquals(res.get(0).get(i).getHeader().getCreatorID(), res.get(k).get(i).getHeader().getCreatorID());
            }
        }

    }
//
@Test
void TestStressFourServersMuteFault() throws InterruptedException {
    Thread.sleep(timeToWaitBetweenTests);
    Thread[] servers = new Thread[4];
    int nnodes = 4;
    String fourServersConfig = Paths.get("config", "bbcConfig", "bbcFourServers").toString();
    logger.info("start TestStressFourServersNoFailures");

    Node[] allNodes = initLocalRmfNodes(nnodes,1,fourServersConfig, null);
    for (int i = 0 ; i < nnodes ; i++) {
        int finalI = i;
//        if (i == 0) {
//            servers[i]  = new Thread(() ->((byzantineBcServer) allNodes[finalI]).start());
//            servers[i].start();
//            continue;
//        }
        servers[i]  = new Thread(() ->((bcServer) allNodes[finalI]).start());
        servers[i].start();
    }
    for (int i = 0 ; i < nnodes ; i++) {
        servers[i].join();
    }

    for (int i = 1 ; i < nnodes ; i++) {
        ((bcServer) allNodes[i]).serve();
    }

    HashMap<Integer, ArrayList<Block>> res = new HashMap<>();
    for (int i = 1 ; i < nnodes ; i++) {
        res.put(i, new ArrayList<Block>());
    }
    Thread[] tasks = new Thread[4];
    for (int i = 1 ; i < 4 ;i++) {
        int finalI = i;
        tasks[i] = new Thread(() -> serverDoing(res.get(finalI), (bcServer) allNodes[finalI]));
        tasks[i].start();
    }
    for (int i = 1 ; i < nnodes ; i++) {
        tasks[i].join();
    }

    for (int i = 0 ; i < 4 ; i++) {
        ((bcServer) allNodes[i]).shutdown();
    }

    for (int i = 0 ; i < 100 ; i++) {
        logger.info(format("***** Assert creator = %d", res.get(1).get(i).getHeader().getCreatorID()));
        for (int k = 1 ; k < nnodes ;k++) {
            assertEquals(res.get(1).get(i).getHeader().getCreatorID(), res.get(k).get(i).getHeader().getCreatorID());
        }
    }

}
    void bServerDoing(List<Block> res, byzantineBcServer s) {
        for(int i = 0; i < 100 ; i++) {
            String msg = "Hello" + i;
            if (i % 4 == s.getID()) {
                s.addTransaction(msg.getBytes(), 0);
            }
            res.add(s.deliver(i));
        }
    }
    @Test
    void TestStressFourServersSelectiveBroadcastFault() throws InterruptedException {
        Thread.sleep(timeToWaitBetweenTests);
        Thread[] servers = new Thread[4];
        int nnodes = 4;
        String fourServersConfig = Paths.get("config", "bbcConfig", "bbcFourServers").toString();
        logger.info("start TestStressFourServersNoFailures");

        Node[] allNodes = initLocalRmfNodes(nnodes,1,fourServersConfig, new int[]{0});
        for (int i = 0 ; i < nnodes ; i++) {
            int finalI = i;
            if (i == 0) {
                servers[i]  = new Thread(() ->((byzantineBcServer) allNodes[finalI]).start());
                servers[i].start();
                continue;
            }
            servers[i]  = new Thread(() ->((bcServer) allNodes[finalI]).start());
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
            ((bcServer) allNodes[i]).serve();
        }

        HashMap<Integer, ArrayList<Block>> res = new HashMap<>();
        for (int i = 0 ; i < nnodes ; i++) {
            res.put(i, new ArrayList<Block>());
        }
        Thread[] tasks = new Thread[4];
        for (int i = 0 ; i < 4 ;i++) {
            if (i == 0) {
                int finalI1 = i;
                tasks[i] = new Thread(() -> bServerDoing(res.get(finalI1), (byzantineBcServer) allNodes[finalI1]));
                tasks[i].start();
                continue;
            }
            int finalI = i;
            tasks[i] = new Thread(() -> serverDoing(res.get(finalI), (bcServer) allNodes[finalI]));
            tasks[i].start();
        }
        for (int i = 0 ; i < nnodes ; i++) {
            tasks[i].join();
        }

        for (int i = 0 ; i < 4 ; i++) {
            if (i == 0) {
                ((byzantineBcServer) allNodes[i]).shutdown();
                continue;
            }
            ((bcServer) allNodes[i]).shutdown();
        }

        for (int i = 0 ; i < 100 ; i++) {
            logger.info(format("***** Assert creator = %d", res.get(0).get(i).getHeader().getCreatorID()));
            for (int k = 0 ; k < nnodes ;k++) {
                assertEquals(res.get(0).get(i).getHeader().getCreatorID(), res.get(k).get(i).getHeader().getCreatorID());
            }
        }

    }

    void asyncServerDoing(List<Block> res, asyncBcServer s) {
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
//        Thread.sleep(timeToWaitBetweenTests);
        Thread[] servers = new Thread[4];
        int nnodes = 4;
        String fourServersConfig = Paths.get("config", "bbcConfig", "bbcFourServers").toString();
        logger.info("start TestStressFourServersNoFailures");

        Node[] allNodes = initLocalAsyncRmfNodes(nnodes,1,fourServersConfig, null);

        for (int i = 0 ; i < nnodes ; i++) {
            int finalI = i;
            servers[i]  = new Thread(() ->((asyncBcServer) allNodes[finalI]).start());
            servers[i].start();
        }
        for (int i = 0 ; i < nnodes ; i++) {
            servers[i].join();
        }

        for (int i = 0 ; i < nnodes ; i++) {
            ((asyncBcServer) allNodes[i]).serve();
        }
        HashMap<Integer, ArrayList<Block>> res = new HashMap<>();
        for (int i = 0 ; i < nnodes ; i++) {
            res.put(i, new ArrayList<Block>());
        }
        Thread[] tasks = new Thread[4];
        for (int i = 0 ; i < 4 ;i++) {
            int finalI = i;
            tasks[i] = new Thread(() -> asyncServerDoing(res.get(finalI), (asyncBcServer) allNodes[finalI]));
            tasks[i].start();
        }
        for (int i = 0 ; i < nnodes ; i++) {
            tasks[i].join();
        }

        for (int i = 0 ; i < 100 ; i++) {
            logger.info(format("***** Assert creator = %d", res.get(0).get(i).getHeader().getCreatorID()));
            for (int k = 0 ; k < nnodes ;k++) {
                assertEquals(res.get(0).get(i).getHeader().getCreatorID(), res.get(k).get(i).getHeader().getCreatorID());
            }
        }

        for (int i = 0 ; i < 4 ; i++) {
            ((asyncBcServer) allNodes[i]).shutdown();
        }



    }

//    @Test
//    void TestFourServersSplitBroadcastFault() throws InterruptedException {
//        Thread.sleep(timeToWaitBetweenTests);
//        Thread[] servers = new Thread[4];
//        int nnodes = 4;
//        String fourServersConfig = Paths.get("config", "bbcConfig", "bbcFourServers").toString();
//        logger.info("start TestStressFourServersNoFailures");
//
//        Node[] allNodes = initLocalRmfNodes(nnodes,1,fourServersConfig, new int[]{0});
//        for (int i = 0 ; i < nnodes ; i++) {
//            int finalI = i;
//            if (i == 0) {
//                servers[i]  = new Thread(() ->((byzantineBcServer) allNodes[finalI]).start());
//                servers[i].start();
//                continue;
//            }
//            servers[i]  = new Thread(() ->((bcServer) allNodes[finalI]).start());
//            servers[i].start();
//        }
//        for (int i = 0 ; i < nnodes ; i++) {
//            servers[i].join();
//        }
//
//        for (int i = 0 ; i < nnodes ; i++) {
//            if (i == 0) {
//                ((byzantineBcServer) allNodes[i]).setFullByz();
//                ((byzantineBcServer) allNodes[i]).serve();
//                continue;
//            }
//            ((bcServer) allNodes[i]).serve();
//        }
//
//        Thread.sleep(10 * 1000);
//        for (int i = 0 ; i < 4 ; i++) {
//            if (i == 0) {
//                ((byzantineBcServer) allNodes[i]).shutdown();
//                continue;
//            }
//            ((bcServer) allNodes[i]).shutdown();
//        }
//    }
}
