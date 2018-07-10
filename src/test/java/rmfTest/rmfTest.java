package rmfTest;

import config.Config;
import config.Node;
import org.junit.jupiter.api.Test;
import rmf.RmfNode;

import java.nio.file.Paths;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class rmfTest {
    interface nodeDoing {
        void act(RmfNode node);
    }
    static Config conf = new Config();
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(rmfTest.class);
    private String localHost = "127.0.0.1";
    private int[] ports = {20000, 20010, 20020, 20030};
    private Node s0= new Node(localHost, ports[0], 0);
    private Node s1= new Node(localHost, ports[1], 1);
    private Node s2= new Node(localHost, ports[2], 2);
    private Node s3= new Node(localHost, ports[3], 3);
    private ArrayList<Node> nodes = new ArrayList<Node>() {{add(s0); add(s1); add(s2); add(s3);}};


    private RmfNode[] initLocalRmfNodes(int nnodes, int f, String configHome) {
        ArrayList<Node> currentNodes = new ArrayList<>(nodes.subList(0, nnodes));
        RmfNode[] ret = new RmfNode[nnodes];
        for (int id = 0 ; id < nnodes ; id++) {
            logger.info("init server #" + id);
            ret[id] =  new RmfNode(id, localHost, ports[id], f, 100, 1000 * 1, currentNodes, configHome);
        }
        return ret;
        //        n.start();

    }
    @Test
    void TestSingleServer() {
        logger.info("start TestSingleServer");
        String SingleServerconfigHome = Paths.get("config", "bbcConfig", "bbcSingleServer").toString();
        RmfNode[] rn1 = initLocalRmfNodes(1, 0, SingleServerconfigHome);
//        rn1.start();
        rn1[0].startService();
        rn1[0].stop();
    }

    @Test
    void TestSingleServerDisseminateMessage() {
        logger.info("start TestSingleServerDisseminateMessage");
        String SingleServerconfigHome = Paths.get("config", "bbcConfig", "bbcSingleServer").toString();
        RmfNode[] rn1 = initLocalRmfNodes( 1, 0, SingleServerconfigHome);
        rn1[0].startService();
        String msg = "hello world";
        rn1[0].broadcast(msg.getBytes(), 0);
        byte[] msgR = rn1[0].deliver(0);
        rn1[0].stop();
        assertEquals(msg, new String(msgR));
    }

    @Test
    void TestFourServersNoFailuresSingleMessage() {
        nodeDoing rec = node -> {
            String msg = "Hello";
            int height = 0;
            node.startService();
            assertEquals(msg, new String(node.deliver(height)));
        };

        nodeDoing sender = node -> {
            String msg = "Hello";
            int height = 0;
            node.startService();
            node.broadcast(msg.getBytes(), height);
        };
        int nnodes = 4;
        String fourServersConfig = Paths.get("config", "bbcConfig", "bbcFourServers").toString();
        logger.info("start TestFourServersNoFailures");

        RmfNode[] allNodes = initLocalRmfNodes(nnodes,1,fourServersConfig);
        try {
            Thread.sleep(5 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sender.act(allNodes[0]);
        Thread[] tasks = new Thread[4];
        for (int i = 0 ; i < nnodes ; i++) {
            int finalI = i;
            tasks[i] = new Thread(()-> rec.act(allNodes[finalI]));
            tasks[i].start();
        }
        for (int i = 0 ; i < nnodes ; i++) {
//            int finalI = i;
            try {
                tasks[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    void TestFourServersSenderMuteFailure() {
        nodeDoing rec = node -> {
//            String msg = "Hello";
            int height = 0;
            node.startService();
            assertNull(node.deliver(height));
        };
        int nnodes = 4;
        logger.info("start TestFourServersNoFailures");
        String fourServersConfig = Paths.get("config", "bbcConfig", "bbcFourServers").toString();
        RmfNode[] allNodes = initLocalRmfNodes(nnodes,1,fourServersConfig);
        try {
            Thread.sleep(5 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Thread[] tasks = new Thread[4];
        for (int i = 0 ; i < nnodes ; i++) {
            int finalI = i;
            tasks[i] = new Thread(()-> rec.act(allNodes[finalI]));
            tasks[i].start();
        }
        for (int i = 0 ; i < nnodes ; i++) {
//            int finalI = i;
            try {
                tasks[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
