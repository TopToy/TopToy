package app;

import com.google.protobuf.ByteString;
import config.Config;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import proto.Types;
import proto.clientServiceGrpc;
import utils.CSVUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static java.lang.String.format;

public class ClientTester {
    private static org.apache.log4j.Logger logger;
    static clientServiceGrpc.clientServiceBlockingStub stub;

    static int clientID;
    static String serverIP;
    static int serverPort = 9876;
    static int testTXS = 0;
    static int latency = 0;
    static int txNum = 0;
    static int txSize = 0;
    static int serverID = 0;
    static long  testTime = 0;
    static Queue<Integer> txTimes = new LinkedList<>();
    static Queue<Types.txID> txIds = new LinkedList<>();

    public static void main(String[] argv) {
        clientID = Integer.parseInt(argv[0]);
        serverID = Integer.parseInt(argv[1]);
        testTXS = Integer.parseInt(argv[2]);
        Config.setConfig(null, clientID);
        logger = org.apache.log4j.Logger.getLogger(ClientTester.class);
        serverIP = Config.getIP(serverID);
        txSize = Config.getTxSize();

        start();
        test();
        try {
            collectResults();
            collectSummery();
        } catch (IOException e) {
            logger.error(e);
        }

    }


    static void start() {
        stub = clientServiceGrpc.newBlockingStub(
                ManagedChannelBuilder.forAddress(serverIP, serverPort).usePlaintext().build()
        );
    }

    static void test() {
        testTime = System.currentTimeMillis();
        while(txNum < testTXS) {
            long txStart = System.currentTimeMillis();
            SecureRandom random = new SecureRandom();
            byte[] tx = new byte[txSize];
            random.nextBytes(tx);
            Types.txID tid = stub.write(Types.Transaction.newBuilder()
                    .setData(ByteString.copyFrom(tx))
                    .setClientID(clientID)
                    .build());
            Types.txStatus sts = stub.status(Types.readReq.newBuilder().setTid(tid)
                    .setBlocking(true).build());
            if (sts.getRes() != 0) {
                logger.info("An uncommitted transaction");
                continue;
            }
            int txLatency = (int) (System.currentTimeMillis() - txStart);
            txIds.add(tid);
            txTimes.add(txLatency);
            latency += txLatency;
            txNum++;

        }
        testTime = System.currentTimeMillis() - testTime;
    }

    static void collectResults() throws IOException {
        String pathString = "/tmp/JToy/res/";
        Path path = Paths.get(pathString,   String.valueOf(JToy.s.getID()), "csummery.csv");
        File f = new File(path.toString());
        if (!f.exists()) {
            f.getParentFile().mkdirs();
            f.createNewFile();
        }
        FileWriter writer = null;
        writer = new FileWriter(path.toString(), true);

        double tlatency = 0;
        if (txNum > 0) {
            tlatency = (double) (latency / txNum);
        }
        List<String> row = Arrays.asList(
                String.valueOf(clientID)
                , String.valueOf(serverID)
                , String.valueOf(txSize)
                , String.valueOf(testTime / 1000)
                , String.valueOf(latency)
                , String.valueOf(txNum)
                , String.valueOf(tlatency)
        );
        CSVUtils.writeLine(writer, row);
        writer.flush();
        writer.close();
    }

    static void collectSummery() throws IOException {
        String pathString = "/tmp/JToy/res/";
        Path path = Paths.get(pathString,   String.valueOf(JToy.s.getID()), "ctsummery.csv");
        File f = new File(path.toString());
        if (!f.exists()) {
            f.getParentFile().mkdirs();
            f.createNewFile();
        }
        FileWriter writer = null;
        writer = new FileWriter(path.toString(), true);
        List<List<String>> data = new LinkedList<>();
        int n = 0;
        while (txTimes.size() > 0) {
            Types.txID tid = txIds.remove();
            int txTime = txTimes.remove();
            data.add(Arrays.asList(
                    String.valueOf(n)
                    , String.valueOf(clientID)
                    , String.valueOf(serverID)
                    , format("[%d%d%d%d]", tid.getChannel(), tid.getProposerID(), tid.getBid(), tid.getTxNum())
                    , String.valueOf(txSize)
                    , String.valueOf(txTime)
            ));
        }

        CSVUtils.writeLines(writer, data);
        writer.flush();
        writer.close();
    }
}
