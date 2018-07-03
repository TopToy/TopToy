package rmfTest;

import config.Config;
import config.Node;
import org.junit.jupiter.api.Test;
import rmf.RmfNode;

import java.nio.file.Paths;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class rmfTest {
    static Config conf = new Config();
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(rmfTest.class);
    private String localHost = "127.0.0.1";
    private int s0Port = 20000;
    private int s1Port = 20010;
    private int s2Port = 20020;
    private int s3Port = 20030;
    int[] ports = {20000, 20010, 20020, 20030};
    private Node s0= new Node(localHost, ports[0], 0);
    private Node s1= new Node(localHost, ports[1], 1);
    private Node s2= new Node(localHost, ports[2], 2);
    private Node s3= new Node(localHost, ports[3], 3);
    private ArrayList<Node> nodes = new ArrayList<Node>() {{add(s0); add(s1); add(s2); add(s3);}};


    private RmfNode initLocalRmfNode(int id, int nnodes, int f, String configHome) {
        logger.info("init server #" + id);
        ArrayList<Node> currentNodes = new ArrayList<>(nodes.subList(0, nnodes));
        //        n.start();
        return new RmfNode(id, localHost, ports[id], f, 100, 1000 * 1, currentNodes, configHome);
    }
    @Test
    void TestSingleServer() {
        logger.info("start TestSingleServer");
        String SingleServerconfigHome = Paths.get("config", "bbcConfig", "bbcSingleServer").toString();
        RmfNode rn1 = initLocalRmfNode(0, 1, 0, SingleServerconfigHome);
//        rn1.start();
        rn1.startService();
        rn1.stop();
    }

    @Test
    void TestSingleServerDisseminateMessage() {
        logger.info("start TestSingleServerDisseminateMessage");
        String SingleServerconfigHome = Paths.get("config", "bbcConfig", "bbcSingleServer").toString();
        RmfNode rn1 = initLocalRmfNode(0, 1, 0, SingleServerconfigHome);
        rn1.startService();
        String msg = "hello world";
        rn1.broadcast(msg.getBytes(), 0);
        byte[] msgR = rn1.deliver(0);
        rn1.stop();
        assertEquals(msg, new String(msgR));
    }

    @Test
    void TestFourServersNoFailures() {
        logger.info("start TestFourServersNoFailures");
        String fourServersConfig = Paths.get("config", "bbcConfig", "bbcFourServers").toString();
        RmfNode rn0 = initLocalRmfNode(0, 4, 1, fourServersConfig);
        RmfNode rn1 = initLocalRmfNode(1, 4, 1, fourServersConfig);
        RmfNode rn2 = initLocalRmfNode(2, 4, 1, fourServersConfig);
        RmfNode rn3 = initLocalRmfNode(3, 4, 1, fourServersConfig);

        rn0.startService();
        rn1.startService();
        rn2.startService();
        rn3.startService();

        String msg = "Hello";
        rn0.broadcast(msg.getBytes(), 0);
        assertEquals(msg, new String(rn0.deliver(0)));
        assertEquals(msg, new String(rn1.deliver(0)));
        assertEquals(msg, new String(rn2.deliver(0)));
        assertEquals(msg, new String(rn3.deliver(0)));
    }
}
