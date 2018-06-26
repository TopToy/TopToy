package bbcTest;

import consensus.bbc.bbcClient;
import consensus.bbc.bbcServer;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class bbcTest {
    private Path SingleServerconfigHome = Paths.get("config", "bbcConfig", "bbcSingleServer");
    private Path FourServerconfigHome = Paths.get("config", "bbcConfig", "bbcFourServers");

    @Test
    void initServerTest() {
        bbcServer s = new bbcServer(0, 1, SingleServerconfigHome.toString());
    }
    @Test
    void initClientTets() {
        bbcServer s = new bbcServer(0, 1, SingleServerconfigHome.toString());
        bbcClient c = new bbcClient(0, SingleServerconfigHome.toString());
        c.close();
    }
    @Test
    void testSingleDecision() {
        bbcServer s = new bbcServer(0, 1, SingleServerconfigHome.toString());
        bbcClient c = new bbcClient(0, SingleServerconfigHome.toString());
        c.propose(1,0);
        int d = s.decide(0);
        assertEquals(d, 1);
        d = c.propose(0,1);
        assertEquals(d,0);
    }
    @Test
    void testFourServersAllCorrect1() throws InterruptedException {
        int consID = 0;
        bbcServer s0 = new bbcServer(0, 3, FourServerconfigHome.toString());
        bbcServer s1 = new bbcServer(1, 3, FourServerconfigHome.toString());
        bbcServer s2 = new bbcServer(2, 3, FourServerconfigHome.toString());
        bbcServer s3 = new bbcServer(3, 3, FourServerconfigHome.toString());

        new Thread(s0::start).start();
        Thread.sleep(1000 * 5);
        new Thread(s1::start).start();
        Thread.sleep(1000 * 5);
        new Thread(s2::start).start();
        Thread.sleep(1000 * 5);
        new Thread(s3::start).start();
        Thread.sleep(1000 * 5);
        bbcClient c0 = new bbcClient(0, FourServerconfigHome.toString());
        bbcClient c1 = new bbcClient(1, FourServerconfigHome.toString());
        bbcClient c2 = new bbcClient(2, FourServerconfigHome.toString());
        bbcClient c3 = new bbcClient(3, FourServerconfigHome.toString());
        c0.propose(1,consID);
        c1.propose(1,consID);
        c2.propose(1,consID);
        c3.propose(1,consID);
        assertEquals(1, s0.decide(consID));
        assertEquals(1, s1.decide(consID));
        assertEquals(1, s2.decide(consID));
        assertEquals(1, s3.decide(consID));

        consID++;
        c0.propose(0,consID);
        c1.propose(0,consID);
        c2.propose(0,consID);
        c3.propose(0,consID);
        assertEquals(0, s0.decide(consID));
        assertEquals(0, s1.decide(consID));
        assertEquals(0, s2.decide(consID));
        assertEquals(0, s3.decide(consID));

        consID++;
        c0.propose(1,consID);
        c1.propose(0,consID);
        c2.propose(0,consID);
        c3.propose(0,consID);
        assertEquals(0, s0.decide(consID));
        assertEquals(0, s1.decide(consID));
        assertEquals(0, s2.decide(consID));
        assertEquals(0, s3.decide(consID));

        consID++;
        c0.propose(1,consID);
        c1.propose(1,consID);
        c2.propose(0,consID);
        c3.propose(0,consID);
        int res = s0.decide(consID);
        System.out.println("Consensus result is " + res);
//        assertEquals(s0.decide(consID), 0);
        assertEquals(res, s1.decide(consID));
        assertEquals(res, s2.decide(consID));
        assertEquals(res, s3.decide(consID));

        consID++;

        c0.propose(1,consID);
        c1.propose(1,consID);
        c2.propose(1,consID);
        c3.propose(0,consID);
        assertEquals(1, s0.decide(consID));
        assertEquals(1, s1.decide(consID));
        assertEquals(1, s2.decide(consID));
        assertEquals(1, s3.decide(consID));

    }

    @Test
    void testFourServersBasicFault1() throws InterruptedException {
        int consID = 0;
        bbcServer s0 = new bbcServer(0, 3, FourServerconfigHome.toString());
        bbcServer s1 = new bbcServer(1, 3, FourServerconfigHome.toString());
        bbcServer s2 = new bbcServer(2, 3, FourServerconfigHome.toString());
        bbcServer s3 = new bbcServer(3, 3, FourServerconfigHome.toString());

        new Thread(s0::start).start();
        Thread.sleep(1000 * 5);
        new Thread(s1::start).start();
        Thread.sleep(1000 * 5);
        new Thread(s2::start).start();
        Thread.sleep(1000 * 5);
        new Thread(s3::start).start();
        Thread.sleep(1000 * 5);
        bbcClient c0 = new bbcClient(0, FourServerconfigHome.toString());
        bbcClient c1 = new bbcClient(1, FourServerconfigHome.toString());
        bbcClient c2 = new bbcClient(2, FourServerconfigHome.toString());
        bbcClient c3 = new bbcClient(3, FourServerconfigHome.toString());
//        c0.propose(1,consID);
        c1.propose(1, consID);
        c2.propose(1, consID);
        c3.propose(1, consID);
        assertEquals(1, s0.decide(consID));
        assertEquals(1, s1.decide(consID));
        assertEquals(1, s2.decide(consID));
        assertEquals(1, s3.decide(consID));
    }

    @Test
    void testFourServersBasicFault2() throws InterruptedException {
        int consID = 0;
        bbcServer s0 = new bbcServer(0, 3, FourServerconfigHome.toString());
        bbcServer s1 = new bbcServer(1, 3, FourServerconfigHome.toString());
        bbcServer s2 = new bbcServer(2, 3, FourServerconfigHome.toString());
        bbcServer s3 = new bbcServer(3, 3, FourServerconfigHome.toString());

//        new Thread(s0::start).start();
//        Thread.sleep(1000 * 5);
        new Thread(s1::start).start();
        Thread.sleep(1000 * 5);
        new Thread(s2::start).start();
        Thread.sleep(1000 * 5);
        new Thread(s3::start).start();
        Thread.sleep(1000 * 5);
//        bbcClient c0 = new bbcClient(0, FourServerconfigHome.toString());
        bbcClient c1 = new bbcClient(1, FourServerconfigHome.toString());
        bbcClient c2 = new bbcClient(2, FourServerconfigHome.toString());
        bbcClient c3 = new bbcClient(3, FourServerconfigHome.toString());
//        c0.propose(1,consID);
        c1.propose(1, consID);
        c2.propose(1, consID);
        c3.propose(1, consID);
//        assertEquals(1, s0.decide(consID));
        assertEquals(1, s1.decide(consID));
        assertEquals(1, s2.decide(consID));
        assertEquals(1, s3.decide(consID));
    }

}
