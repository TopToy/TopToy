package app;

import com.google.protobuf.ByteString;
import config.Config;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.StrictMath.max;
import static java.lang.String.format;

public class ClientTester {
    private static org.apache.log4j.Logger logger;
    private static clientServiceGrpc.clientServiceBlockingStub[] stubs;

    private static int clientID;
    private static int serverPort = 9876;
    private static int testTXS = 0;
    private static AtomicInteger latency = new AtomicInteger(0);
    private static AtomicInteger txNum = new AtomicInteger(0);
    private static AtomicInteger maxLatency = new AtomicInteger(0);
    private static int txSize = 0;
    private static long  testTime = 0;
    private static int n;
    private static ConcurrentLinkedQueue<Integer> txTimes = new ConcurrentLinkedQueue<>();
    private static ConcurrentLinkedQueue<Types.txID> txIds = new ConcurrentLinkedQueue<>();
    private static ExecutorService clients;

    private static AtomicBoolean recorded = new AtomicBoolean(false);
    private static final Object notifier = new Object();


    public static void main(String[] argv) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!recorded.get()) {
                try {
                    collectResults();
                    collectSummery();
                } catch (IOException e) {
                    logger.error(e);
                }

            }
        }));

        clientID = Integer.parseInt(argv[0]);
        testTXS = Integer.parseInt(argv[2]);
        Config.setConfig(null, clientID);
        logger = org.apache.log4j.Logger.getLogger(ClientTester.class);
        txSize = Config.getTxSize();
        n = Config.getN();
        clients = Executors.newFixedThreadPool(n);

        start();
        testTime = System.currentTimeMillis();
        for (int i = 0 ; i < n ; i++) {
            int finalI = i;
            clients.submit(() -> test(stubs[finalI], finalI));

        }
        synchronized (notifier) {
            try {
                while (txNum.get() < testTXS) {
                    notifier.wait();
                }
            } catch (Exception e) {
                logger.error(e);
            }

        }

        clients.shutdownNow();
        try {
            collectResults();
            collectSummery();
            recorded.set(true);
        } catch (IOException e) {
            logger.error(e);
        }
        System.out.println("Client goodbye :)");

    }


    private static void start() {
        logger.info("init clients");
        stubs = new clientServiceGrpc.clientServiceBlockingStub[n];
        for (int i = 0 ; i < n ; i++) {
            stubs[i] = clientServiceGrpc.newBlockingStub(
                    ManagedChannelBuilder
                            .forAddress(Config.getIP(i), serverPort)
                            .usePlaintext()
                            .build());
        }
    }

    private static void test(clientServiceGrpc.clientServiceBlockingStub stub, int server) {
        logger.info("Start client for " + server);
        stub.write(Types.Transaction.newBuilder() // This meant to remove the effect of connecting to the server
                .setData(ByteString.copyFrom(new byte[0]))
                .setClientID(clientID)
                .build());

        while(txNum.get() < testTXS) {
            long txStart = System.currentTimeMillis();
            SecureRandom random = new SecureRandom();
            byte[] tx = new byte[txSize];
            random.nextBytes(tx);
            Types.txID tid = stub.write(Types.Transaction.newBuilder()
                    .setData(ByteString.copyFrom(tx))
                    .setClientID(clientID)
                    .build());
            Types.txStatus sts = stub.status(Types.readReq.newBuilder().setCid(clientID).setTid(tid)
                    .setBlocking(true).build());
            if (sts.getRes() != 0) {
                logger.info("An uncommitted transaction");
                continue;
            }
            int txLatency = (int) (System.currentTimeMillis() - txStart);
            txIds.add(tid);
            txTimes.add(txLatency);
            latency.addAndGet(txLatency);
            maxLatency.set(max(maxLatency.get() , txLatency));
            txNum.incrementAndGet();

        }
        synchronized (notifier) {
            notifier.notifyAll();
        }
        logger.info("Client " + server +" is done");
    }

    private static void collectResults() throws IOException {
        testTime = System.currentTimeMillis() - testTime;
        String pathString = "/tmp/JToy/res/";
        Path path = Paths.get(pathString,   String.valueOf(clientID), "csummery.csv");
        File f = new File(path.toString());
        if (!f.exists()) {
            f.getParentFile().mkdirs();
            f.createNewFile();
        }
        FileWriter writer = null;
        writer = new FileWriter(path.toString(), true);

        double tlatency = 0;
        if (txNum.get() > 0) {
            tlatency = (double) (latency.get() / txNum.get());
        }
        List<String> row = Arrays.asList(
                String.valueOf(Config.getC())
                , String.valueOf(Config.getMaxTransactionsInBlock())
                , String.valueOf(clientID)
                , String.valueOf(txSize)
                , String.valueOf(testTime / 1000)
                , String.valueOf(txNum)
                , String.valueOf(tlatency)
                , String.valueOf(maxLatency.get())
        );
        CSVUtils.writeLine(writer, row);
        writer.flush();
        writer.close();
    }

    private static void collectSummery() throws IOException {
        String pathString = "/tmp/JToy/res/";
        Path path = Paths.get(pathString,   String.valueOf(clientID), "ctsummery.csv");
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
                    , String.valueOf(Config.getC())
                    , String.valueOf(Config.getMaxTransactionsInBlock())
                    , String.valueOf(clientID)
                    , format("[%d%d%d%d]", tid.getChannel(), tid.getProposerID(), tid.getBid(), tid.getTxNum())
                    , String.valueOf(txSize)
                    , String.valueOf(txTime)
            ));
            n++;
        }

        CSVUtils.writeLines(writer, data);
        writer.flush();
        writer.close();
    }
}
