package bbcTest;

import config.Config;
import consensus.bbc.bbcClient;
import consensus.bbc.bbcServer;
import org.junit.jupiter.api.Test;
import proto.FastBbcVote;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class bbcTest {
    interface serverDoing {
        void act(bbcServer s);
    }

    interface clientDoing {
        void act(bbcClient c);
    }
    int timeToWaitBetweenTest = 10 * 1000;
    static Config conf = new Config();
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(bbcTest.class);
    private static Path SingleServerconfigHome = Paths.get("config", "bbcConfig", "bbcSingleServer");
    private static Path FourServerconfigHome = Paths.get("config", "bbcConfig", "bbcFourServers");

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
    @Test
    void initServerTest() throws InterruptedException {
        logger.info("Testing initServerTest...");
        deleteViewIfExist(SingleServerconfigHome.toString());
        bbcServer s = new bbcServer(0, 1, SingleServerconfigHome.toString());
        serverDoing sDoing = bbcServer::start;
        Thread t = new Thread(()->sDoing.act(s));
        t.start();
        Thread.sleep(5 * 1000);
        s.shutdown();
//        Thread.sleep(timeToWaitBetweenTest);
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
    @Test
    void initClientTets() throws InterruptedException {
        logger.info("Testing initClientTets...");
        deleteViewIfExist(SingleServerconfigHome.toString());
        bbcServer s = new bbcServer(0, 1, SingleServerconfigHome.toString());
        bbcClient c = new bbcClient(0, SingleServerconfigHome.toString());
        c.close();
//        Thread.sleep(timeToWaitBetweenTest);
    }
    @Test
    void testSingleDecision() throws InterruptedException {
        logger.info("Testing testSingleDecision...");
        deleteViewIfExist(SingleServerconfigHome.toString());
        bbcServer s = new bbcServer(0, 1, SingleServerconfigHome.toString());
        serverDoing sDo = bbcServer::start;
        Thread t1 = new Thread(()->sDo.act(s));
        t1.start();
        Thread.sleep(5 * 1000);
        clientDoing cDo = c -> {
            c.propose(1, 0);
            c.propose(0,1);
        };

        bbcClient c = new bbcClient(0, SingleServerconfigHome.toString());
        Thread t2 = new Thread(()->cDo.act(c));
        t2.start();

        serverDoing sDo2 = s1 -> {
            assertEquals(1, s1.decide(0));
            assertEquals(0, s1.decide(1));
        };

        sDo2.act(s);

        c.close();
        s.shutdown();
//        Thread.sleep(timeToWaitBetweenTest);
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
    @Test
    void testFourServersAllCorrect1() throws InterruptedException {
        logger.info("Testing testFourServersAllCorrect1...");
        deleteViewIfExist(FourServerconfigHome.toString());
        int consID = 0;

        Thread[] serversThread = new Thread[4];
        bbcServer[] servers = new bbcServer[4];
        for (int i = 0 ; i < 4 ; i++) {
            servers[i] = new bbcServer(i, 3, FourServerconfigHome.toString());
            int finalI = i;
            serversThread[i] = new Thread(() -> servers[finalI].start());
            serversThread[i].start();
        }
        Thread.sleep(5 * 1000);
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
//        for (int i = 0 ; i < 4 ; i++) {
//            serversThread[i].join();
//        }
        logger.info("End of testFourServersAllCorrect1");

    }
    @Test
    void testFourServersBasicFault1() throws InterruptedException {
        logger.info("Testing testFourServersBasicFault1...");
        deleteViewIfExist(FourServerconfigHome.toString());
        int consID = 0;

        Thread[] serversThread = new Thread[4];
        bbcServer[] servers = new bbcServer[4];
        for (int i = 0 ; i < 4 ; i++) {
            servers[i] = new bbcServer(i, 3, FourServerconfigHome.toString());
            int finalI = i;
            serversThread[i] = new Thread(() -> servers[finalI].start());
            serversThread[i].start();
        }
        Thread.sleep(5 * 1000);
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
//        for (int i = 0 ; i < 4 ; i++) {
//            serversThread[i].join();
//        }

    }

    @Test
    void stressTestFourServersAllCorrect() throws InterruptedException {
        logger.info("Testing testFourServersAllCorrect1...");
        deleteViewIfExist(FourServerconfigHome.toString());
        int consID = 0;

        Thread[] serversThread = new Thread[4];
        bbcServer[] servers = new bbcServer[4];
        for (int i = 0 ; i < 4 ; i++) {
            servers[i] = new bbcServer(i, 3, FourServerconfigHome.toString());
            int finalI = i;
            serversThread[i] = new Thread(() -> servers[finalI].start());
            serversThread[i].start();
        }
        Thread.sleep(5 * 1000);
        bbcClient[] clients = new bbcClient[4];
        for (int i = 0 ; i < 4 ; i++) {
            clients[i] = new bbcClient(i, FourServerconfigHome.toString());

        }
        for (int i = 0 ; i < 100 ; i++) {
            for (int k = 0 ; k < 4 ; k++) {
                clients[k].propose(consID % 2, consID);
            }
            for (int k = 0 ; k < 4 ; k++) {
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

//        for (int i = 0 ; i < 4 ; i++) {
//            serversThread[i].join();
//        }
        logger.info("End of stressTestFourServersAllCorrect");
//        Thread.sleep(timeToWaitBetweenTest);
    }
}
