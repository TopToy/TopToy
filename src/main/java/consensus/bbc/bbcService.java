package consensus.bbc;

import bftsmart.communication.client.ReplyListener;
import bftsmart.tom.AsynchServiceProxy;
import bftsmart.tom.MessageContext;
import bftsmart.tom.RequestContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;
import crypto.pkiUtils;
import proto.BbcProtos;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

/*
    TODO:
    1. Truncate the executed consensuses (or swap them to db)
 */
public class bbcService extends DefaultSingleRecoverable {
    class consVote {
        int pos = 0;
        int neg = 0;
        BbcProtos.BbcDecision.Builder dec = BbcProtos.BbcDecision.newBuilder();
    }


    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(bbcService.class);

    private int id;
    AsynchServiceProxy bbcProxy;
    private final HashMap<Integer, consVote> rec;
//    private List<Integer> done;
//    private Lock lock;
//    private Condition consEnd;
    private int quorumSize;
    private String configHome;
    private ServiceReplica sr;
    private final List<Integer> consNotify;
    public bbcService(int id, int quorumSize, String configHome) {
        this.id = id;
        rec = new HashMap<>();
//        lock = new ReentrantLock();
//        consEnd = lock.newCondition();
        sr = null;
        this.quorumSize = quorumSize;
        this.configHome = configHome;
        consNotify = new ArrayList<>();
    }

    public void start() {
//        try {
            sr = new ServiceReplica(id, this, this, configHome);
            bbcProxy = new AsynchServiceProxy(id, configHome);
//        } catch (Exception ex) {
//            logger.error("", ex);
//        }

         Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                if (sr != null) {
                    logger.warn(format("[#%d] shutting down bbc server since JVM is shutting down", id));
                    sr.kill();
                    logger.warn(format("[#%d] server shut down", id));
                }
            }
        });
    }
    public void shutdown() {
        logger.info(format("[#%d] shutting down bbc server", id));
        releaseWaiting();
        if (bbcProxy != null) {
            bbcProxy.close();
            logger.info(format("[#%d] shut down bbc client successfully", id));
        }
        if (sr != null) {
            sr.kill();
            logger.info(format("[#%d] bbc server has been shutting down successfully", id));
            sr = null;
        }
    }

    void releaseWaiting() {
        synchronized (rec) {
            rec.notifyAll();
        }
        synchronized (consNotify) {
            consNotify.notifyAll();
        }
    }
    @Override
    public void installSnapshot(byte[] state) {
        logger.info(format("[#%d] installSnapshot called", id));
//        state newState = (consensus.bbc.state) SerializationUtils.deserialize(state);
//        quorumSize = newState.quorumSize;
//        rec = newState.rec;
    }

    @Override
    public byte[] getSnapshot() {
        logger.info(format("[#%d] getSnapshot called", id));
//        state newState = new state(rec, quorumSize);
//        byte[] data = SerializationUtils.serialize(newState);
//        return data;
        return new byte[1];
    }

    @Override
    public byte[] appExecuteOrdered(byte[] command, MessageContext msgCtx) {
        int cid;
        try {
            BbcProtos.BbcMsg msg = BbcProtos.BbcMsg.parseFrom(command);
            cid = msg.getId();
            logger.debug(format("[#%d] received bbc message from [#%d]", id, msg.getClientID()));
            synchronized (rec) {
                if (!rec.containsKey(cid)) {
                    consVote v = new consVote();
                    v.dec.setConsID(cid);
                    rec.put(cid, v);
                }
                consVote curr = rec.get(cid);
                if (curr.neg +  curr.pos < quorumSize) {
                    if (msg.getVote() == 1) {
                        curr.pos++;
                    } else {
                        curr.neg++;
                    }
                }
                curr.dec.addSignatures(msg.getSig());
                if (curr.neg + curr.pos == quorumSize) {
                    logger.debug(format("[#%d] notify on  [cid=%d]", id, cid));
                    rec.notify();
                }
            }
            synchronized (consNotify) {
                if (!consNotify.contains(cid)) {
                    consNotify.add(cid);
                    consNotify.notify();
                }
            }
        } catch (Exception e) {
            logger.error("", e);
        }
        return new byte[0];
    }

    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
//        int consID = ByteBuffer.wrap(command).getInt();
//        synchronized (rec) {
//            while (!rec.containsKey(consID) ||  rec.get(consID).size() < quorumSize) {
//                try {
//                    rec.wait();
//                } catch (InterruptedException e) {
//                    logger.error("", e);
//                }
//            }
//            int ret = calcMaj(rec.get(consID));
////        done.add(consID);
//            rec.remove(consID);
//            return ByteBuffer.allocate(Integer.SIZE/Byte.SIZE).putInt(ret).array();
//        }
        return new byte[1];
    }

    public BbcProtos.BbcDecision decide(int cid) {
        synchronized (rec) {
            while (!rec.containsKey(cid) || rec.get(cid).pos + rec.get(cid).neg < quorumSize) {
                try {
                    rec.wait();
                } catch (InterruptedException e) {
                    logger.error("", e);
                    return null;
                }
            }
            return (rec.get(cid).pos > rec.get(cid).neg ?
                    rec.get(cid).dec.setDecosion(1).build() : rec.get(cid).dec.setDecosion(0).build());
        }
    }

    public ArrayList<Integer> notifyOnConsensusInstance(List<Integer> req) throws InterruptedException {
        if (req.size() == 0) return new ArrayList<>();
        List<Integer> ret;
        synchronized (consNotify) {
            while (consNotify.isEmpty()) {
                consNotify.wait();
            }
            ret = consNotify.stream().filter(
                    req::contains).
            collect(Collectors.toList());
            consNotify.removeAll(req);
            // TODO: Handle the list size!
        }
        return (ArrayList<Integer>) ret;
    }

    public int propose(int vote, int cid) {
        BbcProtos.BbcMsg.Builder b = BbcProtos.BbcMsg.newBuilder();
        b.setClientID(id);
        b.setId(cid);
        b.setVote(vote);
        b.setSig(pkiUtils.sign(String.valueOf(cid) + String.valueOf(id) + String.valueOf(vote)));
        BbcProtos.BbcMsg msg= b.build();
        byte[] data = msg.toByteArray();
        bbcProxy.invokeAsynchRequest(data, new ReplyListener() {
            @Override
            public void reset() {

            }

            @Override
            public void replyReceived(RequestContext context, TOMMessage reply) {

            }
        }, TOMMessageType.ORDERED_REQUEST);
//        byte[] cmd = ByteBuffer.allocate(Integer.SIZE/Byte.SIZE).putInt(consID).array(); // Blocks until ends
//        byte[] res = vpbcProxy.invokeUnordered(cmd);
//        int ret = ByteBuffer.wrap(res).getInt();
//        consID++;
//        return ret;
        return 0;
    }

}

//class state implements Serializable{
//    public TreeMap<Integer, ArrayList<BbcProtos.BbcMsg>> rec;
//    public int quorumSize;
//
//    public state(TreeMap<Integer, ArrayList<BbcProtos.BbcMsg>> rec, int quorumSize) {
//        this.rec = rec;
//        this.quorumSize = quorumSize;
//    }
//}
