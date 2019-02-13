package app;

import clients.TxClient;
import proto.Types;
import utils.CSVUtils;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.StrictMath.max;
import static java.lang.String.format;
public class ClientsAgent {
    private static org.apache.log4j.Logger logger;
    private static List<TxClient> clients = new ArrayList<>();
//    private static ThreadPoolExecutor executor; // = (ThreadPoolExecutor) Executors.newFixedThreadPool(clients);
    private static AtomicBoolean stopped = new AtomicBoolean(false);
    static int cln = 1;
//    static double total = 0;

    public static void main(String argv[]) {
        int agentID = Integer.parseInt(argv[0]);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy:hh:mm:ss");
        System.setProperty("current.date.time", dateFormat.format(new Date()));
        int s_id = agentID;
        System.setProperty("s_id", Integer.toString(s_id));
        logger = org.apache.log4j.Logger.getLogger(ClientsAgent.class);
        try {
            mainImpl2(argv);
        } catch (Exception e) {
            logger.error("", e);
        }

//        mainImpl1(argv, clientsNum, txSize, time);

    }

    static public void mainImpl2(String argv[]) throws InterruptedException, IOException {
        int agentID = Integer.parseInt(argv[0]);
        int time = Integer.parseInt(argv[1]);
        int tSize = Integer.parseInt(argv[2]);
        int bareTxSize = Types.Transaction.newBuilder()
                .setClientID(0)
                .setId(Types.txID.newBuilder().setTxID(UUID.randomUUID().toString()).build())
                .build().getSerializedSize() + 8;
        int txSize = max(0, tSize - bareTxSize);
        String pathString = argv[3];
        int clientsNum = argv.length - 4;
        logger.info(format("params: [id:%d, time:%d, size:%d, path:%s]", agentID, time, tSize, pathString));
        Stat st = new Stat();

        ExecutorService clients_executor = Executors.newFixedThreadPool(clientsNum * cln);
//        ConcurrentHashMap<Integer, String> submitted = new ConcurrentHashMap<>();
//        ExecutorService deliver_executor = Executors.newFixedThreadPool(1);
//        CountDownLatch latch = new CountDownLatch(1);
//        AtomicInteger sent = new AtomicInteger(0);
        List<Types.approved> approved = new ArrayList<>();
        int clID = 0;
        for (int i = 0 ; i < clientsNum ; i++) {
            for (int j = 0 ; j < cln ; j++) {
                TxClient cl = new TxClient(clID, argv[i + 4], 9876);
                clID++;
                clients.add(cl);
                int finalClID = clID;
                clients_executor.submit(() -> {
                    logger.info(format("starting client: [%d, %d]", agentID, finalClID));
                    while (!stopped.get()) {
//                        long start = System.currentTimeMillis();
                        Types.accepted ret = submitRandTxToServer(cl, txSize);
//                        logger.info(format("sending message took [%d]", System.currentTimeMillis() - start));
                        if (ret == null) {
                            cl.shutdown();
                            return;
                        }
                        if (ret.getAccepted()) {
                            Types.approved app;

//                            logger.info(format("read tx [%s]", ret.getTxID()));
//                            start = System.currentTimeMillis();
                            do {
                                Types.read r = Types.read.newBuilder().setTxID(ret.getTxID()).build();
                                app = cl.getTx(r);
                            } while (app == Types.approved.getDefaultInstance() && !stopped.get());
//                            logger.info(format("read message took [%d]", System.currentTimeMillis() - start));
                            if (stopped.get()) break;
                            synchronized (st) {
                                approved.add(collectSummery(st, app));
                            }
//                            sent.getAndIncrement();
//                            submitted.put(cl.getID(), ret.getTxID());
                        }
                    }
                    logger.info(format("client finished: [%d, %d]", agentID, finalClID));
//                    latch.countDown();
                });
            }
        }

//        deliver_executor.submit(() -> {
//            logger.info("starting delivery");
//            while (!stopped.get() || submitted.size() > 0) {
//                for (Map.Entry<Integer, String> e : submitted.entrySet()) {
//                    Types.read r = Types.read.newBuilder().setTxID(e.getValue()).build();
//                    Types.approved app = clients.get(e.getKey()).getTx(r);
//                    if (app == Types.approved.getDefaultInstance()) continue;
//                    submitted.remove(e.getKey());
//                    collectSummery(st, app);
//                }
//            }
//            logger.info("delivery finishes");
//            latch.countDown();
//        });

        Thread.sleep(time * 1000);
        stopped.set(true);
        Thread.sleep(5 * 1000);
        clients_executor.shutdownNow();
//        latch.await();
//        deliver_executor.shutdownNow();
//
//        st.sent = sent.get();
        writeSummery(pathString, agentID, st);
        writeBlocks(pathString, agentID, approved, st);

    }


    static Types.approved collectSummery(Stat st, Types.approved app) {
        long ts = System.currentTimeMillis();
        st.txCount++;
        st.diff += ts - app.getTx().getClientTs();
        st.diffServer += app.getSt().getDecided() - app.getTx().getServerTs();
        st.txSize = app.getTx().getSerializedSize();
        return app.toBuilder().setSt(app.getSt().toBuilder().setSign(ts)).build(); // We save here the client delivery time
    }

    static void writeSummery(String pathString, int agentID, Stat st) throws IOException {
        Path path = Paths.get(pathString,   String.valueOf(agentID), "summery.csv");
        File f = new File(path.toString());
        if (!f.exists()) {
            f.getParentFile().mkdirs();
            f.createNewFile();
        }
        FileWriter writer = new FileWriter(path.toString(), true);
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy-HH:mm:ss");
        int clientDiffAvg = 0;
        int serverDiffAvg = 0;
        int scDiffAvg = 0;
        if (st.txCount > 0) {
            clientDiffAvg = (int) (st.diff / st.txCount);
            serverDiffAvg = (int) (st.diffServer / st.txCount);
            scDiffAvg = (int) ((st.diff - st.diffServer) / st.txCount);
        }
        List<String> row = Arrays.asList(dateFormat.format(new Date()), String.valueOf(agentID)
                ,String.valueOf(st.txSize), String.valueOf(st.txCount), String.valueOf(clientDiffAvg),
                String.valueOf(serverDiffAvg), String.valueOf(scDiffAvg));
        CSVUtils.writeLine(writer, row);
        writer.flush();
        writer.close();
    }

    static void writeBlocks(String pathString, int agentID, List<Types.approved> approveds, Stat st) throws IOException {
        Path path = Paths.get(pathString,   String.valueOf(agentID), "blocksStat.csv");
        File f = new File(path.toString());
        if (!f.exists()) {
            f.getParentFile().mkdirs();
            f.createNewFile();
        }
        FileWriter writer = new FileWriter(path.toString(), true);
        DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy-HH:mm:ss");
        for (Types.approved app : approveds) {
            Types.blockStatistics bst = app.getSt();
            List<String> row = Arrays.asList(dateFormat.format(new Date()), String.valueOf(agentID)
                    ,String.valueOf(st.txSize), String.valueOf(bst.getSign() - app.getTx().getClientTs()),
                    String.valueOf(bst.getDecided() - app.getTx().getServerTs()),
                    String.valueOf((bst.getSign() - app.getTx().getClientTs()) - (bst.getDecided() - app.getTx().getServerTs())));
            CSVUtils.writeLine(writer, row);
        }
        writer.flush();
        writer.close();
    }

    static Types.accepted submitRandTxToServer(TxClient c, int txSize) {
//        byte[] ts = Longs.toByteArray(System.currentTimeMillis());
        SecureRandom random = new SecureRandom();
        byte[] tx = new byte[txSize];
        random.nextBytes(tx);
        return c.addTx(tx);
    }

//    static void mainImpl1(String argv[], int clientsNum, int txSize, int time) {
//        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(clientsNum * cln);
//        Random rand = new Random();
////        for (int j = 0 ; j < 5 ; j++) {
//        for (int i = 0 ; i < clientsNum ; i++) {
//            for (int j = 0 ; j < cln ; j++) {
////                System.out.println("Establishing client for " +  argv[i + 2]);
//                int cID = rand.nextInt(10000);
//                TxClient cl = new TxClient(cID, argv[i + 2], 9876);
//                clients.add(cl);
//                executor.submit(() -> {
//                    while (!stopped.get()) {
//                        if (submitRandTxToServer(cl, txSize) == null) {
//                            cl.shutdown();
//                            return;
//                        }
//                    }
//                });
//            }
//        }
//        logger.info(format("Total of %d clients", clients.size()));
//        try {
//            logger.info(format("sleeps for %d", time));
//            Thread.sleep(time * 1000);
//            stopped.set(true);
//            Thread.sleep(2 * 1000);
//
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        int total2 = 0;
//        for (TxClient c : clients) {
//            total += c.shutdown();
//            logger.info(format("Client %d send %d tx in total with throughput of %s", c.getID(),
//                    c.getTxCount(), String.valueOf(c.getAvgTx())));
//        }
//        logger.info(format("shutting down executor"));
//        executor.shutdown();
//        logger.info(format("total %d ; tx/sec %s", total2, String.valueOf(total)));
//        System.exit(0);
//    }
}
class Stat {
    int txCount = 0;
    long diff = 0;
    long diffServer = 0;
    long txSize = 0;
}
