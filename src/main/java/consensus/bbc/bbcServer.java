package consensus.bbc;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;
import protos.BbcProtos;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class bbcServer extends DefaultSingleRecoverable {
    private Map<Integer, ArrayList<BbcProtos.BbcMsg>> rec;
    private List<Integer> done;
    private Lock lock;
    private Condition consEnd;
    private int quorumSize;

    public bbcServer(int id, int quorumSize, String configHome) {
        rec = new HashMap<>();
        done = new ArrayList<>();
        lock = new ReentrantLock();
        consEnd = lock.newCondition();
        this.quorumSize = quorumSize;
        new ServiceReplica(id, this, this, configHome);
    }

    @Override
    public void installSnapshot(byte[] state) {
        System.out.println("installSnapshot currently not implemented");
    }

    @Override
    public byte[] getSnapshot() {
        System.out.println("getSnapshot currently not implemented");
        return new byte[0];
    }

    @Override
    public byte[] appExecuteOrdered(byte[] command, MessageContext msgCtx) {
        lock.lock();
        try {
            BbcProtos.BbcMsg msg = BbcProtos.BbcMsg.parseFrom(command);
            int key = msg.getConsID();
            if (done.contains(key)) {
                lock.unlock();
                return new byte[0];
            }
            if (rec.containsKey(key)) {
                rec.get(key).add(msg);
            } else {
                ArrayList<BbcProtos.BbcMsg> newList = new ArrayList<>();
                newList.add(msg);
                rec.put(key, newList);
            }
            if (rec.get(key).size() >= quorumSize) {
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
        System.out.println("appExecuteUnordered currently not implemented");
        return new byte[0];
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
        while (rec.get(consID).size() < quorumSize) {
            try {
                consEnd.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        int ret = calcMaj(rec.get(consID));
        done.add(consID);
        rec.remove(consID);
        lock.unlock();
        return ret;
    }

}
