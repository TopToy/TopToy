package consensus.bbc;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;
import org.apache.commons.lang.SerializationUtils;
import proto.BbcProtos;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static java.lang.String.format;

/*
    TODO:
    1. Truncate the executed consensuses (or swap them to db)
 */
public class bbcServer extends DefaultSingleRecoverable {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(bbcServer.class);

    private int id;
    private final TreeMap<Integer, ArrayList<BbcProtos.BbcMsg>> rec;
//    private List<Integer> done;
//    private Lock lock;
//    private Condition consEnd;
    private int quorumSize;
    private String configHome;
    private ServiceReplica sr;
    private final List<Integer> consNotify;
    public bbcServer(int id, int quorumSize, String configHome) {
        this.id = id;
        rec = new TreeMap<>();
//        lock = new ReentrantLock();
//        consEnd = lock.newCondition();
        sr = null;
        this.quorumSize = quorumSize;
        this.configHome = configHome;
        consNotify = new ArrayList<>();
    }

    public void start() {
        try {
            sr = new ServiceReplica(id, this, this, configHome);
        } catch (Exception ex) {
            logger.error("", ex);
        }

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
            int key = msg.getConsID();
            synchronized (consNotify) {
                consNotify.add(key);
                consNotify.notify();
            }
            synchronized (rec) {
                if (rec.containsKey(key)) {
                    if (rec.get(key).size() < quorumSize) {
                        rec.get(key).add(msg);
                    }
                } else {
                    ArrayList<BbcProtos.BbcMsg> newList = new ArrayList<>();
                    newList.add(msg);
                    rec.put(key, newList);
                }
                if (rec.get(key).size() == quorumSize) {
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
        int consID = ByteBuffer.wrap(command).getInt();
        synchronized (rec) {
            while (!rec.containsKey(consID) ||  rec.get(consID).size() < quorumSize) {
                try {
                    rec.wait();
                } catch (InterruptedException e) {
                    logger.error("", e);
                }
            }
            int ret = calcMaj(rec.get(consID));
//        done.add(consID);
            rec.remove(consID);
            return ByteBuffer.allocate(Integer.SIZE/Byte.SIZE).putInt(ret).array();
        }
    }

    private int calcMaj(ArrayList<BbcProtos.BbcMsg> rec) {
        int ones = Collections.
                frequency(
                        rec.
                                stream().
                                map(BbcProtos.BbcMsg::getVote).
                                collect(Collectors.toList())
                        , 1);
        int zeros = Collections.
                frequency(
                        rec.
                                stream().
                                map(BbcProtos.BbcMsg::getVote).
                                collect(Collectors.toList())
                        , 0);

        if (ones > zeros) return 1;
        else return 0;
    }

    public int decide(int consID) {
        synchronized (rec) {
            while (!rec.containsKey(consID) ||  rec.get(consID).size() < quorumSize) {
                try {
                    rec.wait();
                } catch (InterruptedException e) {
                    logger.error("", e);
                }
            }
            int ret = calcMaj(rec.get(consID));
//        done.add(consID);
            rec.remove(consID);
            return ret;
        }
    }

    public ArrayList<Integer> notifyOnConsensusInstance() {
        ArrayList<Integer> ret;
        synchronized (consNotify) {
            while (consNotify.isEmpty()) {
                try {
                    consNotify.wait();
                } catch (InterruptedException e) {
                    logger.warn("", e);
                }
            }
            ret = new ArrayList<>(consNotify);
            consNotify.clear();
        }
        return ret;
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
