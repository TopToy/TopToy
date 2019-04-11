package das.ms;

import das.ab.ABService;
import das.data.Data;
import proto.Types;

import java.util.concurrent.CountDownLatch;

public class Membership {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Membership.class);

    private static CountDownLatch startLetch;

    public Membership(int n) {
//        System.out.println("initiate with " + n);
        startLetch = new CountDownLatch(n);
    }

    static public void start() {
        ABService.broadcast(Types.startMsg.newBuilder().build().toByteArray(), Types.Meta.newBuilder().build(), Data.RBTypes.START);
        try {
            startLetch.await();
        } catch (InterruptedException e) {
            logger.fatal(e);
        }
    }

    static public void handleStartMsg() {
//        System.out.println("down");
        startLetch.countDown();
    }
}
