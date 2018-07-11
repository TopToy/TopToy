package rmfTest;

import config.Config;
import config.Node;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.checkerframework.dataflow.qual.TerminatesExecution;
import org.junit.jupiter.api.Test;
import rmf.ByzantineRmfNode;
import rmf.RmfNode;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class rmfTest {
    interface nodeDoing {
        void act(RmfNode node);
    }
    private int serversUp = 0;
    private final Object serversUpLock = new Object();
    private int timeToWaitBetweenTests = 1000 * 15;
    static Config conf = new Config();
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(rmfTest.class);
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
                ret[id] = new ByzantineRmfNode(id, localHost, ports[id], f, 1, 1000 * 1, currentNodes, configHome);
                continue;
            }
            ret[id] =  new RmfNode(id, localHost, ports[id], f, 1, 1000 * 1, currentNodes, configHome);
        }
        return ret;
        //        n.start();

    }
    @Test
    void TestSingleServer() throws InterruptedException {
        Thread.sleep(timeToWaitBetweenTests);
        serversUp = 0;
        logger.info("start TestSingleServer");
        String SingleServerconfigHome = Paths.get("config", "bbcConfig", "bbcSingleServer").toString();
        Node[] rn1 = initLocalRmfNodes(1, 0, SingleServerconfigHome, null);
        RmfNode rn = ((RmfNode) rn1[0]);
        ((RmfNode) rn1[0]).start();
        ((RmfNode) rn1[0]).stop();
    }

    @Test
    void TestSingleServerDisseminateMessage() throws InterruptedException {
        Thread.sleep(timeToWaitBetweenTests);
        logger.info("start TestSingleServerDisseminateMessage");
        String SingleServerconfigHome = Paths.get("config", "bbcConfig", "bbcSingleServer").toString();
        Node[] rn = initLocalRmfNodes( 1, 0, SingleServerconfigHome, null);
        ((RmfNode) rn[0]).start();
        String msg = "hello world";
        ((RmfNode) rn[0]).broadcast(msg.getBytes(), 0);
        assertEquals(msg, new String(((RmfNode) rn[0]).deliver(0, 0)));
        ((RmfNode) rn[0]).stop();

    }

    @Test
    void TestFourServersNoFailuresSingleMessage() throws InterruptedException {
        Thread.sleep(timeToWaitBetweenTests);
        Thread[] servers = new Thread[4];
        int nnodes = 4;
        String fourServersConfig = Paths.get("config", "bbcConfig", "bbcFourServers").toString();
        logger.info("start TestFourServersNoFailures");

        Node[] allNodes = initLocalRmfNodes(nnodes,1,fourServersConfig, null);
        for (int i = 0 ; i < nnodes ; i++) {
            int finalI = i;
            servers[i]  = new Thread(() ->((RmfNode) allNodes[finalI]).start());
            servers[i].start();
        }
        for (int i = 0 ; i < nnodes ; i++) {
            servers[i].join();
        }
        int height = 0;
        String msg = "Hello";
        ((RmfNode) allNodes[0]).broadcast(msg.getBytes(), 0);
//        sender.act((RmfNode) allNodes[0]);
        String[] ret = new String[4];
        Thread[] tasks = new Thread[4];
        for (int i = 0 ; i < nnodes ; i++) {
            int finalI = i;
            tasks[i] = new Thread(()-> {
                ret[finalI] = new String(((RmfNode) allNodes[finalI]).deliver(height, 0));
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
            ((RmfNode) allNodes[i]).stop();
        }
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
            servers[i]  = new Thread(() ->((RmfNode) allNodes[finalI]).start());
            servers[i].start();
        }
        for (int i = 0 ; i < nnodes ; i++) {
            servers[i].join();
        }
        for (int k = 0 ; k < 100 ; k++) {
            String msg = "Hello" + k;
            ((RmfNode) allNodes[k % 4]).broadcast(msg.getBytes(), k);
//        sender.act((RmfNode) allNodes[0]);
            String[] ret = new String[4];
            Thread[] tasks = new Thread[4];
            for (int i = 0 ; i < nnodes ; i++) {
                int finalI = i;
                int finalK = k;
                tasks[i] = new Thread(()-> {
                    ret[finalI] = new String(((RmfNode) allNodes[finalI]).deliver(finalK, finalK % 4));
                });
                tasks[i].start();
            }

            for (int i = 0 ; i < nnodes ; i++) {
                tasks[i].join();
            }

            for (int i = 0 ; i < 4 ; i++) {
//                logger.info(format("******** round [%d:%d], message is [%s:%s] *********", k, i,  msg, ret[i]));
                assertEquals(ret[0], ret[i]);
            }
        }

        for (int i = 0 ; i < 4 ; i++) {
            ((RmfNode) allNodes[i]).stop();
        }
    }

    @Test
    void TestFourServersSenderMuteFailure() throws InterruptedException {
        Thread.sleep(timeToWaitBetweenTests);
        Thread[] servers = new Thread[4];
        int nnodes = 4;
        String fourServersConfig = Paths.get("config", "bbcConfig", "bbcFourServers").toString();
        logger.info("start TestFourServersSenderMuteFailure");

        Node[] allNodes = initLocalRmfNodes(nnodes,1,fourServersConfig, null);
        for (int i = 0 ; i < nnodes ; i++) {
            int finalI = i;
            servers[i]  = new Thread(() ->((RmfNode) allNodes[finalI]).start());
            servers[i].start();
        }
        for (int i = 0 ; i < nnodes ; i++) {
            servers[i].join();
        }
        int height = 0;
        String[] ret = new String[4];
        Thread[] tasks = new Thread[4];
        for (int i = 0 ; i < nnodes ; i++) {
            int finalI = i;
            tasks[i] = new Thread(()-> {
                byte[] res = ((RmfNode) allNodes[finalI]).deliver(height, 0);
                ret[finalI] = (res == null ? null : new String(res));
            });
            tasks[i].start();
        }

        for (int i = 0 ; i < nnodes ; i++) {
            tasks[i].join();
        }

        for (int i = 0 ; i < 4 ; i++) {
            assertNull(ret[i]);
//            ((RmfNode) allNodes[i]).stop();
        }
        for (int i = 0 ; i < 4 ; i++) {
            ((RmfNode) allNodes[i]).stop();
        }
    }
    @Test
    void TestFourServersSelectiveBroadcast() throws InterruptedException {
        Thread.sleep(timeToWaitBetweenTests);
        Thread[] servers = new Thread[4];
        int nnodes = 4;
        String fourServersConfig = Paths.get("config", "bbcConfig", "bbcFourServers").toString();
        logger.info("start TestFourServersSelectiveBroadcast");

        Node[] allNodes = initLocalRmfNodes(nnodes,1,fourServersConfig, new int[] {0});
        for (int i = 0 ; i < nnodes ; i++) {
            int finalI = i;
            if (i == 0) {
                servers[i] =  new Thread(() ->((ByzantineRmfNode) allNodes[finalI]).start());
            } else {
                servers[i]  = new Thread(() ->((RmfNode) allNodes[finalI]).start());
            }
            servers[i].start();
        }
        for (int i = 0 ; i < nnodes ; i++) {
            servers[i].join();
        }
        int height = 0;
        String msg = "Hello";
        ((ByzantineRmfNode) allNodes[0]).selectiveBroadcast(msg.getBytes(), 0, new int[] {1, 2});
//        sender.act((RmfNode) allNodes[0]);
        String[] ret = new String[4];
        Thread[] tasks = new Thread[4];
        for (int i = 0 ; i < nnodes ; i++) {
            int finalI = i;
            if (i == 0) {
                tasks[i] = new Thread(()-> {
                    byte[] res = ((ByzantineRmfNode) allNodes[finalI]).deliver(height, 0);
                    ret[finalI] = (res == null ? null : new String(res));
                });

            } else {
                tasks[i] = new Thread(()-> {
                    byte[] res = ((RmfNode) allNodes[finalI]).deliver(height, 0);
                    ret[finalI] = (res == null ? null : new String(res));
                });
            }

            tasks[i].start();
        }

        for (int i = 0 ; i < nnodes ; i++) {
            tasks[i].join();
        }

        for (int i = 0 ; i < 4 ; i++) {
            assertEquals(ret[0], ret[i]);
//            ((RmfNode) allNodes[i]).stop();
        }
        for (int i = 0 ; i < 4 ; i++) {
            if (i == 0)  {
                ((ByzantineRmfNode) allNodes[i]).stop();
                continue;
            }
            ((RmfNode) allNodes[i]).stop();
        }

    }

    @Test
    void TestStressFourServersSelectiveBroadcast() throws InterruptedException {
        Thread.sleep(timeToWaitBetweenTests);
        Thread[] servers = new Thread[4];
        int nnodes = 4;
        String fourServersConfig = Paths.get("config", "bbcConfig", "bbcFourServers").toString();
        logger.info("start TestFourServersSelectiveBroadcast");

        Node[] allNodes = initLocalRmfNodes(nnodes,1,fourServersConfig, new int[] {0});
        for (int i = 0 ; i < nnodes ; i++) {
            int finalI = i;
            if (i == 0) {
                servers[i] =  new Thread(() ->((ByzantineRmfNode) allNodes[finalI]).start());
            } else {
                servers[i]  = new Thread(() ->((RmfNode) allNodes[finalI]).start());
            }
            servers[i].start();
        }
        for (int i = 0 ; i < nnodes ; i++) {
            servers[i].join();
        }
        for (int k = 0 ; k < 100 ; k++) {
            String msg = "Hello" + k;
            ((ByzantineRmfNode) allNodes[0]).selectiveBroadcast(msg.getBytes(), k, new int[] {1, 2});
//        sender.act((RmfNode) allNodes[0]);
            String[] ret = new String[4];
            Thread[] tasks = new Thread[4];
            for (int i = 0 ; i < nnodes ; i++) {
                int finalI = i;
                if (i == 0) {
                    int finalK = k;
                    tasks[i] = new Thread(()-> {
                        byte[] res = ((ByzantineRmfNode) allNodes[finalI]).deliver(finalK, 0);
                        ret[finalI] = (res == null ? null : new String(res));
                    });

                } else {
                    int finalK1 = k;
                    tasks[i] = new Thread(()-> {
                        byte[] res = ((RmfNode) allNodes[finalI]).deliver(finalK1, 0);
                        ret[finalI] = (res == null ? null : new String(res));
                    });
                }

                tasks[i].start();
            }

            for (int i = 0 ; i < nnodes ; i++) {
                tasks[i].join();
            }

            for (int i = 0 ; i < 4 ; i++) {
                assertEquals(ret[0], ret[i]);
//            ((RmfNode) allNodes[i]).stop();
            }
        }

        for (int i = 0 ; i < 4 ; i++) {
            if (i == 0)  {
                ((ByzantineRmfNode) allNodes[i]).stop();
                continue;
            }
            ((RmfNode) allNodes[i]).stop();
        }

    }

}
