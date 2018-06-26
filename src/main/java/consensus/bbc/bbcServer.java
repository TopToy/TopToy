package consensus.bbc;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;
import org.apache.commons.lang.SerializationUtils;
import proto.BbcProtos;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/*
    TODO:
    1. Truncate the executed consensuses (or swap them to db)
 */
public class bbcServer extends DefaultSingleRecoverable {
    private int id;
    private TreeMap<Integer, ArrayList<BbcProtos.BbcMsg>> rec;
//    private List<Integer> done;
    private Lock lock;
    private Condition consEnd;
    private int quorumSize;
    private String configHome;

    public bbcServer(int id, int quorumSize, String configHome) {
        this.id = id;
        rec = new TreeMap<>();
//        done = new ArrayList<>();
        lock = new ReentrantLock();
        consEnd = lock.newCondition();
        this.quorumSize = quorumSize;
        this.configHome = configHome;

    }

    public void start() {
        new ServiceReplica(id, this, this, configHome);
    }

    @Override
    public void installSnapshot(byte[] state) {
        System.out.println("installSnapshot called");
        state newState = (consensus.bbc.state) SerializationUtils.deserialize(state);
        quorumSize = newState.quorumSize;
        rec = newState.rec;
    }

    @Override
    public byte[] getSnapshot() {
        System.out.println("getSnapshot called");
        state newState = new state(rec, quorumSize);
        byte[] data = SerializationUtils.serialize(newState);
        return data;
    }

    @Override
    public byte[] appExecuteOrdered(byte[] command, MessageContext msgCtx) {
        lock.lock();
        try {
            BbcProtos.BbcMsg msg = BbcProtos.BbcMsg.parseFrom(command);
            int key = msg.getConsID();
//            if (done.contains(key)) {
//                lock.unlock();
//                return new byte[0];
//            }
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
                consEnd.signal();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        lock.unlock();
        return new byte[0];
    }

    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
        int consID = ByteBuffer.wrap(command).getInt();
        lock.lock();
        while (!rec.containsKey(consID) ||  rec.get(consID).size() < quorumSize) {
            try {
                consEnd.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        int ret = calcMaj(rec.get(consID));
//        done.add(consID);
        rec.remove(consID);
        lock.unlock();
        return ByteBuffer.allocate(Integer.SIZE/Byte.SIZE).putInt(ret).array();
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
        lock.lock();
        while (!rec.containsKey(consID) ||  rec.get(consID).size() < quorumSize) {
            try {
                consEnd.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        int ret = calcMaj(rec.get(consID));
//        done.add(consID);
        rec.remove(consID);
        lock.unlock();
        return ret;
    }

}

class state implements Serializable{
    public TreeMap<Integer, ArrayList<BbcProtos.BbcMsg>> rec;
    public int quorumSize;

    public state(TreeMap<Integer, ArrayList<BbcProtos.BbcMsg>> rec, int quorumSize) {
        this.rec = rec;
        this.quorumSize = quorumSize;
    }
}
