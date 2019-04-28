package das.ms;

import com.google.protobuf.InvalidProtocolBufferException;
import das.ab.ABService;
import das.data.Data;
import proto.Types;

import java.util.concurrent.CountDownLatch;

import static java.lang.String.format;

public class Membership {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Membership.class);

    private static CountDownLatch startLetch;

    public Membership(int n) {
//        System.out.println("initiate with " + n);
        startLetch = new CountDownLatch(n);
    }

    static public int start(int id) {
        ABService.broadcast(Types.startMsg.newBuilder().setId(id).build().toByteArray(), Types.Meta.newBuilder().build(), Data.RBTypes.START);
        try {
            startLetch.await();
        } catch (InterruptedException e) {
            logger.error(e);
            return -1;
        }
        return 0;
    }

    static public void handleStartMsg(Types.RBMsg msg) throws InvalidProtocolBufferException {
        Types.startMsg m = Types.startMsg.parseFrom(msg.getData());
        logger.info(format("Received start msg from [%d]", m.getId()));
        startLetch.countDown();
    }
}