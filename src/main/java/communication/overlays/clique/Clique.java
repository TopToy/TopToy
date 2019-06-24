package communication.overlays.clique;

import communication.CommLayer;
import communication.data.Data;
import utils.Node;
import proto.types.block.*;
import proto.types.comm.*;

import java.util.*;
import java.util.stream.Collectors;

import static crypto.BlockDigSig.verfiyBlockWRTheader;
import static java.lang.String.format;

public class Clique implements CommLayer {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Clique.class);



    private int id;
    private int n;
    private CliqueRpcs rpcs;

    public Clique(int id, int workers, int n,  ArrayList<Node> nodes) {
        this.id = id;
        this.n = n;
        new Data(n, workers);
        rpcs = new CliqueRpcs(id, nodes, n, workers);
    }

    @Override
    public void join() {
        rpcs.start();
    }

    @Override
    public void leave() {
       rpcs.shutdown();
    }

    @Override
    public void broadcast(int worker, Block data) {
        logger.debug(format("[%d-%d] broadcast block data [bid=%d]", id, worker, data.getId().getBid()));
        rpcs.broadcast(worker, data);
    }

    // Meant to test Byzantine activity

    @Override
    public void send(int worker, Block data, int[] recipients) {
        rpcs.send(worker, data, recipients);
    }

    private Block getBlockFromData(int channel, BlockID bid, BlockHeader proof) {
        final Block[] res = {null};
        int pid = bid.getPid();
        Data.blocks[pid][channel].computeIfPresent(bid, (k, v) -> {
            v = v.stream()
                    .filter(b -> verfiyBlockWRTheader(b, proof)).collect(Collectors.toCollection(LinkedList::new));
            res[0] = v.poll();
            return v;
        });
        return res[0];
    }

    @Override
    public Block recBlock(int channel, BlockHeader proof) throws InterruptedException {
        BlockID bid = proof.getBid();
        Block res = getBlockFromData(channel, bid, proof);
        if (res != null) return res;
        rpcs.broadcastCommReq(CommReq.newBuilder().setProof(proof).build());
        int pid = bid.getPid();
        synchronized (Data.blocks[pid][channel]) {
            while (res == null) {
                Data.blocks[pid][channel].wait();
                res = getBlockFromData(channel, bid, proof);
            }
        }
        return res;
    }

    @Override
    public boolean contains(int channel, BlockHeader proof) {
        if (proof.getEmpty()) return true;
        BlockID bid = proof.getBid();
        int pid = bid.getPid();
        Data.blocks[pid][channel].computeIfPresent(bid, (k, v) -> {
            int bef = v.size();
            v = v.stream().filter(b -> verfiyBlockWRTheader(b, proof))
                    .collect(Collectors.toCollection(LinkedList::new));
            logger.debug(format("[%d-%d] invalidate %d records [pid=%d ; bid=%d]", id, channel, bef - v.size(),
                    bid.getPid(), bid.getBid()));
            return v;
        });
        return Data.blocks[pid][channel].containsKey(bid) && Data.blocks[pid][channel].get(bid).size() > 0;
    }

}
