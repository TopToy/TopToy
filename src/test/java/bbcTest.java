import config.Config;
import consensus.bbc.bbcClient;
import consensus.bbc.bbcServer;
import org.junit.jupiter.api.Test;
import proto.BbcProtos;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class bbcTest {
    interface serverDoing {
        void act(bbcServer s);
    }

    interface clientDoing {
        void act(bbcClient c);
    }
    private CountDownLatch latch;
    private int timeToWaitBetweenTest = 15 * 1000;
    static Config conf = new Config();
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(bbcTest.class);
    private static Path SingleServerconfigHome = Paths.get("config", "bbcConfig", "bbcSingleServer");
    private static Path FourServerconfigHome = Paths.get("config", "bbcConfig", "bbcFourServers");
//    private Integer serversUp = 0;
//    private final Object serverLock = new Object();
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
//    void updateServersUp(int sNum) {
//        synchronized (serverLock) {
//            serversUp++;
//            if (serversUp == sNum) {
//                serverLock.notify();
//            }
//        }
//    }
//
//    void waitForServersUp(int num) throws InterruptedException {
//        synchronized (serverLock) {
//            while (serversUp < num) {
//                serverLock.wait();
//            }
//        }
//    }
    @Test
    void initServerTest() throws InterruptedException {
        Thread.sleep(timeToWaitBetweenTest);
        logger.info("Testing initServerTest...");
//        serversUp = 0;
        latch = new CountDownLatch(1);
        deleteViewIfExist(SingleServerconfigHome.toString());
        bbcServer s = new bbcServer(0, 1, SingleServerconfigHome.toString());
        Thread t = new Thread(()-> {
            s.start();
            latch.countDown();
        });
        t.start();
        latch.await();
        s.shutdown();
        t.join();

    }
    @Test
    void initClientTets() throws InterruptedException {
        Thread.sleep(timeToWaitBetweenTest);
        logger.info("Testing initClientTets...");
        deleteViewIfExist(SingleServerconfigHome.toString());
        bbcServer s = new bbcServer(0, 1, SingleServerconfigHome.toString());
        bbcClient c = new bbcClient(0, SingleServerconfigHome.toString());
        c.close();
//        Thread.sleep(timeToWaitBetweenTest);
    }
    @Test
    void testSingleDecision() throws InterruptedException {
        Thread.sleep(timeToWaitBetweenTest);
        logger.info("Testing testSingleDecision...");
//        serversUp = 0;
        latch = new CountDownLatch(1);
        deleteViewIfExist(SingleServerconfigHome.toString());
        bbcServer s = new bbcServer(0, 1, SingleServerconfigHome.toString());
        serverDoing sDo = bbcServer::start;
        Thread t1 = new Thread(()-> {
            sDo.act(s);
//            updateServersUp(1);
            latch.countDown();
        });
        t1.start();
//        waitForServersUp(1);
        latch.await();
        clientDoing cDo = c -> {
            c.propose(1, 0);
            c.propose(0, 1);
        };

        bbcClient c = new bbcClient(0, SingleServerconfigHome.toString());
        Thread t2 = new Thread(()->cDo.act(c));
        t2.start();

        /*
         Asserts is being doing in the main thread which is OK.
         */
        serverDoing sDo2 = s1 -> {
            assertEquals(1, s1.decide(0));
            assertEquals(0, s1.decide(1));
        };

        sDo2.act(s);

        c.close();
        s.shutdown();
//        Thread.sleep(timeToWaitBetweenTest);
        t1.join();
        t2.join();

    }
    @Test
    void testFourServersAllCorrect1() throws InterruptedException {
        Thread.sleep(timeToWaitBetweenTest);
        logger.info("Testing testFourServersAllCorrect1...");
        deleteViewIfExist(FourServerconfigHome.toString());
        int consID = 0;
        latch = new CountDownLatch(4);
        Thread[] serversThread = new Thread[4];
        bbcServer[] servers = new bbcServer[4];
        for (int i = 0 ; i < 4 ; i++) {
            servers[i] = new bbcServer(i, 3, FourServerconfigHome.toString());
            int finalI = i;
            serversThread[i] = new Thread(() -> { servers[finalI].start();
//            updateServersUp(4);
                latch.countDown();
            });
            serversThread[i].start();
        }
//        waitForServersUp(4);
        latch.await();
        bbcClient[] clients = new bbcClient[4];
        for (int i = 0 ; i < 4 ; i++) {
            clients[i] = new bbcClient(i, FourServerconfigHome.toString());
            clients[i].propose(consID % 2, consID);
        }

        for (int i = 0 ; i < 4 ; i++) {
            assertEquals(consID % 2, servers[i].decide(consID));
        }
        consID++;
        for (int i = 0 ; i < 4 ; i++) {
            clients[i].propose(consID % 2, consID);
        }

        for (int i = 0 ; i < 4 ; i++) {
            assertEquals(consID % 2, servers[i].decide(consID));
        }
        for (int i = 0 ; i < 4 ; i++) {
            clients[i].close();
        }

        for (int i = 0 ; i < 4 ; i++) {
            servers[i].shutdown();
        }
//        Thread.sleep(timeToWaitBetweenTest);
        for (int i = 0 ; i < 4 ; i++) {
            serversThread[i].join();
        }
        logger.info("End of testFourServersAllCorrect1");

    }
    @Test
    void testFourServersBasicFault1() throws InterruptedException {
        Thread.sleep(timeToWaitBetweenTest);
        logger.info("Testing testFourServersBasicFault1...");
        deleteViewIfExist(FourServerconfigHome.toString());
        int consID = 0;
        latch = new CountDownLatch(4);
        Thread[] serversThread = new Thread[4];
        bbcServer[] servers = new bbcServer[4];
        for (int i = 0 ; i < 4 ; i++) {
            servers[i] = new bbcServer(i, 3, FourServerconfigHome.toString());
            int finalI = i;
            serversThread[i] = new Thread(() -> {servers[finalI].start();
//                updateServersUp(4);
                latch.countDown();
            });
            serversThread[i].start();
        }
//        waitForServersUp(4);
        latch.await();
        bbcClient[] clients = new bbcClient[4];
        for (int i = 0 ; i < 4 ; i++) {
            clients[i] = new bbcClient(i, FourServerconfigHome.toString());
        }

        for (int i = 0 ; i < 4 ; i++) {
            for (int k = 0 ; k < 4 ; k++) {
                if (i == k) continue;
                clients[k].propose(consID % 2, consID);
            }
            for (int k = 0 ; k < 4 ;k++) {
                assertEquals(consID % 2, servers[k].decide(consID));
            }
            consID++;
        }

        for (int i = 0 ; i < 4 ; i++) {
            clients[i].close();
        }

        for (int i = 0 ; i < 4 ; i++) {
            servers[i].shutdown();
        }
//        Thread.sleep(timeToWaitBetweenTest);
        for (int i = 0 ; i < 4 ; i++) {
            serversThread[i].join();
        }

    }

    @Test
    void stressTestFourServersAllCorrect() throws InterruptedException {
        Thread.sleep(timeToWaitBetweenTest);
        logger.info("Testing testFourServersAllCorrect1...");
        deleteViewIfExist(FourServerconfigHome.toString());
        int consID = 0;
        latch = new CountDownLatch(4);
        Thread[] serversThread = new Thread[4];
        bbcServer[] servers = new bbcServer[4];
        for (int i = 0 ; i < 4 ; i++) {
            servers[i] = new bbcServer(i, 3, FourServerconfigHome.toString());
            int finalI = i;
            serversThread[i] = new Thread(() -> {servers[finalI].start();
//                updateServersUp(4);
                latch.countDown();
            });
            serversThread[i].start();
        }
//        waitForServersUp(4);
        latch.await();
        bbcClient[] clients = new bbcClient[4];
        for (int i = 0 ; i < 4 ; i++) {
            clients[i] = new bbcClient(i, FourServerconfigHome.toString());

        }
        logger.info("***************START OF CONSENSUSES *******************");
        for (int i = 0 ; i < 100 ; i++) {
            for (int k = 0 ; k < 4 ; k++) {
//                logger.info("*************** *******************");
                clients[k].propose(consID % 2, consID);
            }
            for (int k = 0 ; k < 4 ; k++) {
                assertEquals(consID % 2, servers[k].decide(consID));
            }
            consID++;

        }
        logger.info("***************END OF CONSENSUSES *******************");
        for (int i = 0 ; i < 4 ; i++) {
            clients[i].close();
        }

        for (int i = 0 ; i < 4 ; i++) {
            servers[i].shutdown();
        }

        for (int i = 0 ; i < 4 ; i++) {
            serversThread[i].join();
        }
        logger.info("End of stressTestFourServersAllCorrect");
//        Thread.sleep(timeToWaitBetweenTest);
    }
}
