package app;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import proto.types.block.*;
import proto.types.utils;
import utils.config.Config;
import io.grpc.ManagedChannelBuilder;
import proto.crpcs.clientService.ClientServiceGrpc.*;
import utils.CSVUtils;
import proto.types.transaction.*;
import proto.types.client.*;
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
import static proto.crpcs.clientService.ClientServiceGrpc.newBlockingStub;
import static proto.crpcs.clientService.ClientServiceGrpc.newStub;

public class ClientTester {
    private static org.apache.log4j.Logger logger;
    private static ClientServiceBlockingStub[] stubs;

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
    private static ConcurrentLinkedQueue<TxID> txIds = new ConcurrentLinkedQueue<>();
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
        logger.info(format("Going to commit %d transactions", testTXS));
        txSize = Config.getTxSize();
        n = Config.getN();
        clients = Executors.newFixedThreadPool(n);

        start();
        switch (argv[3]) {
            case "p": testP();
                    break;
            case "r" :testRpcs();
                    break;
            default:
                break;

        }

        System.out.println("Client goodbye :)");

    }


    private static void start() {
        logger.info("init clients");
        stubs = new ClientServiceBlockingStub[n];
        for (int i = 0 ; i < n ; i++) {
            stubs[i] = newBlockingStub(
                    ManagedChannelBuilder
                            .forAddress(Config.getIP(i), serverPort)
                            .usePlaintext()
                            .build());
        }
    }

    private static void testRpcs() {
        for (int i = 0 ; i < n ; i++) {
            System.out.println(format("Testing testTxWrite... [%s]",
                    testTxWrite(i) ? "OK" : "Failed"));

            System.out.println(format("Testing testTxRead... [%s]",
                    testTxRead(i) ? "OK" : "Failed"));

            System.out.println(format("Testing testStatus... [%s]",
                    testStatus(i) ? "OK" : "Failed"));

            System.out.println(format("Testing testGetHeight... [%s]",
                    testGetHeight(i) ? "OK" : "Failed"));

            System.out.println(format("Testing testGetBlock... [%s]",
                    testGetBlock(i) ? "OK" : "Failed"));

            System.out.println(format("Testing testIsAlive... [%s]",
                    testIsAlive(i) ? "OK" : "Failed"));

            System.out.println(format("Testing testGetValidators... [%s]",
                    testGetValidators(i) ? "OK" : "Failed"));

            System.out.println(format("Testing testGetConfigData... [%s]",
                    testGetConfigData(i) ? "OK" : "Failed"));

            System.out.println(format("Testing testPoolSize... [%d]",
                    testPoolSize(i)));

            System.out.println(format("Testing testPendingSize... [%d]",
                    testPendingSize(i)));

        }
    }


    private static void testP() {
        testTime = System.currentTimeMillis();
        for (int i = 0 ; i < n ; i++) {
            int finalI = i;
            clients.submit(() -> testPerformace(stubs[finalI], finalI));

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
    }
    private static void testPerformace(ClientServiceBlockingStub stub, int server) {
        logger.info("Start client for " + server);
        stub.writeTx(Transaction.newBuilder() // This meant to remove the effect of connecting to the server
                .setData(ByteString.copyFrom(new byte[0]))
                .setClientID(clientID)
                .build());
        logger.debug("Writing tx");
        while(txNum.get() < testTXS) {
            long txStart = System.currentTimeMillis();
//            SecureRandom random = new SecureRandom();
//            byte[] tx = new byte[txSize];
//            random.nextBytes(tx);
            TxID tid = stub //.withDeadlineAfter(3, TimeUnit.SECONDS)
                    .writeTx(createRandomTransaction());
            if (tid.equals(TxID.getDefaultInstance())) {
                logger.info("received default tid");
                continue;
            }
            TxStatus sts = stub.txStatus(TxReq.newBuilder().setCid(clientID).setTid(tid)
                    .setBlocking(true).build());
            logger.info(format("Transaction status is %s", sts.getStatus()));
            if (!sts.getStatus().equals(TxState.COMMITTED)) {
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
        Path path = Paths.get(pathString,   String.valueOf(clientID), "csummary.csv");
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
        Path path = Paths.get(pathString,   String.valueOf(clientID), "ctsummary.csv");
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
            TxID tid = txIds.remove();
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

    static boolean testTxWrite(int sid) {
//        SecureRandom random = new SecureRandom();
//        byte[] tx = new byte[txSize];
//        random.nextBytes(tx);
        TxID tid = stubs[sid] //.withDeadlineAfter(3, TimeUnit.SECONDS)
                .writeTx(createRandomTransaction());
        return !tid.equals(TxID.getDefaultInstance());
    }

    static boolean testTxRead(int sid) {
        SecureRandom random = new SecureRandom();
        byte[] tx = new byte[txSize];
        random.nextBytes(tx);
        TxID tid = stubs[sid]
                .writeTx(Transaction.newBuilder()
                        .setData(ByteString.copyFrom(tx))
                        .setClientID(clientID)
                        .build());
        Transaction t = stubs[sid].readTx(TxReq.newBuilder().setCid(clientID)
                .setTid(tid).setBlocking(true).build());
        return t.getId().equals(tid) && Arrays.compare(t.getData().toByteArray(), tx) == 0;
    }

    static boolean testStatus(int sid) {
        TxID ftid = TxID
                .newBuilder()
                .setBid(100)
                .setChannel(15)
                .setProposerID(34)
                .setTxNum(90)
                .build();

        if (!stubs[sid]
                .txStatus(TxReq
                        .newBuilder()
                        .setBlocking(false)
                        .setTid(ftid)
                        .setCid(clientID)
                        .build())
                .getStatus().equals(TxState.UNKNOWN))
            return false;

//        SecureRandom random = new SecureRandom();
//        byte[] tx = new byte[txSize];
//        random.nextBytes(tx);
        TxID tid = stubs[sid]
                .writeTx(createRandomTransaction());

        return stubs[sid].txStatus(TxReq.newBuilder().setCid(clientID)
                .setTid(tid)
                .setBlocking(true).build()).getStatus().equals(TxState.COMMITTED);
    }

    static boolean testGetHeight(int sid) {
//        SecureRandom random = new SecureRandom();
//        byte[] tx = new byte[txSize];
//        random.nextBytes(tx);
        stubs[sid]
                .writeTx(createRandomTransaction());
        return stubs[sid].getHeight(utils.Empty.newBuilder().build()).getNum() > 0;

    }
    static boolean testGetBlock(int sid) {
        int h = stubs[sid].getHeight(utils.Empty.newBuilder().build()).getNum();
        for (int i = 0 ; i < h + 1 ; i++) {
            Block b = stubs[sid].readBlock(BlockReq.newBuilder()
                    .setCid(clientID)
                    .setHeight(i)
                    .setBlocking(true).build());
            if (b.getHeader().getHeight() != i) return false;
        }
        return true;
    }

    static boolean testIsAlive(int sid) {
        stubs[sid].isAlive(utils.Empty.newBuilder().build());
        return true;
    }

    static int testPoolSize(int sid) {
        ClientServiceStub asyncStub = newStub(ManagedChannelBuilder
                .forAddress(Config.getIP(sid), serverPort)
                .usePlaintext()
                .build());

        for (int i = 0 ; i < 10000 ; i++) {
            asyncStub.writeTx(createRandomTransaction(), new StreamObserver<TxID>() {
                @Override
                public void onNext(TxID txID) {

                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onCompleted() {

                }
            });
        }


       return stubs[sid].poolSize(utils.Empty.newBuilder().build()).getNum();
    }

    static int testPendingSize(int sid) {
        ClientServiceStub asyncStub = newStub(ManagedChannelBuilder
                .forAddress(Config.getIP(sid), serverPort)
                .usePlaintext()
                .build());

        for (int i = 0 ; i < 10000 ; i++) {
            asyncStub.writeTx(createRandomTransaction(), new StreamObserver<TxID>() {
                @Override
                public void onNext(TxID txID) {

                }

                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onCompleted() {

                }
            });
        }


        return stubs[sid].pendingSize(utils.Empty.newBuilder().build()).getNum();
    }

    static boolean testGetValidators(int sid) {
        Validators val = stubs[sid].getValidators(utils.Empty.newBuilder().build());
        for (int i = 0 ; i < Config.getN() ; i++) {
            if (!val.getIpsList().contains(Config.getIP(i))) return false;
        }
        return true;
    }

    static boolean testGetConfigData(int sid) {
        ConfigInfo c = stubs[sid].getConfigInfo(utils.Empty.newBuilder().build());
        if (c.getClusterSize() != Config.getN()) return false;
        if (c.getMaxFailures() != Config.getF()) return false;
        if (c.getWorkers() !=  Config.getC()) return false;
        if (c.getInitTmo() != Config.getTMO()) return false;
        if (c.getMaxTxInBlock() != Config.getMaxTransactionsInBlock()) return false;
        if (c.getServerID() != sid) return false;
        return true;
    }

    static Transaction createRandomTransaction() {
        SecureRandom random = new SecureRandom();
        byte[] tx = new byte[txSize];
        random.nextBytes(tx);
        return Transaction.newBuilder()
                .setData(ByteString.copyFrom(tx))
                .setClientID(clientID).build();
    }


}
