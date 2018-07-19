package consensus.bbc;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import proto.BbcProtos;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.lang.String.format;

/*
    TODO:
    1. Truncate the executed consensuses (or swap them to db)
 */
public class bbcServer extends DefaultSingleRecoverable {
    class consVote {
        int pos = 0;
        int neg = 0;
    }
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(bbcServer.class);

    private int id;
    private final HashMap<Integer, consVote> rec;
//    private List<Integer> done;
//    private Lock lock;
//    private Condition consEnd;
    private int quorumSize;
    private String configHome;
    private ServiceReplica sr;
    private final List<Integer> consNotify;
    public bbcServer(int id, int quorumSize, String configHome) {
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
        if (sr != null) {
            sr.kill();
            logger.info(format("[#%d] bbc server has been shutting down successfully", id));
            sr = null;
        }
    }

    void releaseWaiting() {
        synchronized (rec) {
            rec.notify();
        }
        synchronized (consNotify) {
            consNotify.notify();
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
        try {
            BbcProtos.BbcMsg msg = BbcProtos.BbcMsg.parseFrom(command);
            int cid = msg.getId();
            synchronized (consNotify) {
                if (!consNotify.contains(cid)) {
                    consNotify.add(cid);
                    consNotify.notify();
                }
            }
            synchronized (rec) {
                consVote v = new consVote();
                if (msg.getVote() == 1) {
                    v.pos++;
                } else {
                    v.neg++;
                }
                consVote curr = rec.get(cid);
                if (curr != null) {
                    if (curr.neg +  curr.pos < quorumSize) {
                        v.pos += curr.pos;
                        v.neg += curr.neg;
                    } else {
                        v = curr;
                    }
                    rec.remove(cid);
                }
                rec.put(cid, v);
                if (v.neg + v.pos == quorumSize) {
                    rec.notify();
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

//    private int calcMaj(ArrayList<BbcProtos.BbcMsg> rec) {
//        int ones = Collections.
//                frequency(
//                        rec.
//                                stream().
//                                map(BbcProtos.BbcMsg::getVote).
//                                collect(Collectors.toList())
//                        , 1);
//        int zeros = Collections.
//                frequency(
//                        rec.
//                                stream().
//                                map(BbcProtos.BbcMsg::getVote).
//                                collect(Collectors.toList())
//                        , 0);
//
//        if (ones > zeros) return 1;
//        else return 0;
//    }

    public int decide(int cid) {
        synchronized (rec) {
//            consVote v = rec.get(height, consID);
            while (!rec.containsKey(cid) || rec.get(cid).pos + rec.get(cid).neg < quorumSize) {
                try {
                    rec.wait();
                } catch (InterruptedException e) {
                    logger.error("", e);
                    return 0;
                }
            }
//            v = rec.get(height, consID);
            //        done.add(consID);
//            rec.remove(consID); TODO: Should handle it!
            return (rec.get(cid).pos > rec.get(cid).neg ? 1 : 0);
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
