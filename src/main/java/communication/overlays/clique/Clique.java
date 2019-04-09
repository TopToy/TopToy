package communication.overlays.clique;

import communication.CommLayer;
import communication.data.Data;
import config.Node;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import proto.CommunicationGrpc;
import proto.Types;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static blockchain.data.BCS.bcs;
import static crypto.blockDigSig.verfiyBlockWRTheader;
import static java.lang.String.format;

public class Clique implements CommLayer {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Clique.class);



    private int id;
    int n;
    CliqueRpcs rpcs;

    public Clique(int id, int workers, int n,  ArrayList<Node> nodes) {
        this.id = id;
        this.n = n;
        new Data(n, workers);
        rpcs = new CliqueRpcs(id, nodes, n);
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
    public void broadcast(int worker, Types.Block data) {
        logger.debug(format("[%d-%d] broadcast block data [bid=%d]", id, worker, data.getId().getBid()));
        rpcs.broadcast(worker, data);
    }

    // Meant to test Byzantine activity

    @Override
    public void send(int worker, Types.Block data, int[] recipients) {
        rpcs.send(worker, data, recipients);
    }

    Types.Block getBlockFromData(int channel, Types.BlockID bid, Types.BlockHeader proof) {
        final Types.Block[] res = {null};
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
    public Types.Block recBlock(int channel, Types.BlockID bid, Types.BlockHeader proof) throws InterruptedException {
        if (proof.getEmpty()) return Types.Block.getDefaultInstance();
        Types.Block res = getBlockFromData(channel, bid, proof);
        if (res != null) return res;
        rpcs.broadcastCommReq(Types.commReq.newBuilder().setProof(proof).build());
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
    public boolean contains(int channel, Types.BlockID bid, Types.BlockHeader proof) {
        if (proof.getEmpty()) return true;
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
