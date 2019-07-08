package das.ms;

import com.google.protobuf.InvalidProtocolBufferException;
import das.ab.ABService;
import das.data.Data;

import java.util.concurrent.CountDownLatch;
import proto.types.meta.*;
import proto.types.fd.*;
import proto.types.rb.*;

import static java.lang.String.format;

public class Membership {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Membership.class);

    private static CountDownLatch startLetch;

    public Membership(int n) {
        startLetch = new CountDownLatch(n);
    }

    static public int start(int id) {
        ABService.broadcast(StartMsg.newBuilder().setId(id).build().toByteArray(), Meta.newBuilder().build(), Data.RBTypes.START);
        try {
            startLetch.await();
        } catch (InterruptedException e) {
            logger.error(e);
            return -1;
        }
        return 0;
    }

    static public void handleStartMsg(RBMsg msg) throws InvalidProtocolBufferException {
        StartMsg m = StartMsg.parseFrom(msg.getData());
        logger.info(format("Received start msg from [%d]", m.getId()));
        startLetch.countDown();
    }

    static public void reconfigure() {

    }
}
