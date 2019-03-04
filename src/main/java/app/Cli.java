package app;
import blockchain.BaseBlockchain;
import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import config.Config;
//import crypto.rmfDigSig;
import crypto.DigestMethod;
import crypto.blockDigSig;
import org.apache.commons.cli.*;
import org.apache.commons.lang.ArrayUtils;
import proto.Types;
import servers.Statistics;
import utils.CSVUtils;
import utils.DBUtils;

import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.min;
import static java.lang.StrictMath.max;
import static java.lang.String.format;
import static java.lang.String.valueOf;

public class Cli {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Cli.class);
        private Options options = new Options();
        private long totalRT = 0;
        static String outPath = "/tmp/JToy/res/";
        public Cli() {
            options.addOption("help", "print this message");
            options.addOption("init", "init the server");
            options.addOption("serve", "start the server\n" +
                    "Note that before lunching the command, all other servers has to be initialized first");
            options.addOption("stop", "terminate the server");
            options.addOption("quit", "exit Toy terminal");
            options.addOption(Option.builder("tx")
                            .hasArg()
                            .desc("-a: add new transaction\n" +
                                    "Usage: tx -a [data]\n" +
                                    "-s: check the status of a transaction\n" +
                                    "Usage: tx -s [txID]")
                            .build()); // TODO: How?
            options.addOption(Option.builder("wait")
                    .hasArg()
                    .desc("Usage: wait [sec]\n" +
                            "waits for [sec] second")
                    .build());
            options.addOption(Option.builder("res")
                    .hasArg()
                    .desc("Write the results into csv file\n" +
                            "Usage: res -p [path_to_csv]")
                    .build());
            options.addOption(Option.builder("bm")
                    .hasArg()
                    .desc("run a benchmark on the system\n" +
                            "Note that this method should run write after init!\n" +
                            "Usage: bm -t [transaction size] -s [amount of loaded transactions] -p [path to csv]")
                    .build());

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                writeSummery(outPath);
//                writeToScv(outPath);
//                writeBlocksStatistics(outPath);
                writeBlocksStatisticsSummery(outPath);
                DBUtils.shutdown();
            }));
        }


        void parse(String[] args) throws InterruptedException, IOException {
//            CommandLineParser parser = new DefaultParser();
//            try {
//                CommandLine line = parser.parse(options, args);
                if (args[0].equals("help")) {
                    help();
                    return;
                }
                if (args[0].equals("init")) {
                    init();
                    System.out.println("Init server... [OK]");
                    return;
                }

//                if (args[0].equals("latency")) {
//                    latency();
//                    System.out.println("latency... [OK]");
//                    return;
//                }

                if (args[0].equals("serve")) {
                    serve();
                    System.out.println("Serving... [OK]");
                    return;
                }
                if (args[0].equals("stop")) {
                    stop();
                    totalRT = System.currentTimeMillis();
                    System.out.println("stop server... [OK]");
                    return;
                }
                if (args[0].equals("quit")) {
//                    writeSummery("/tmp/JToy/res");
                    System.out.println("Goodbye :)");
                    System.exit(0);
                    return;
                }

                if (args[0].equals("wait")) {
                    if (args.length == 2) {
                        int sec = Integer.parseInt(args[1]);
                        if (sec > 0) {

                            System.out.println(format("waits for %d seconds ", sec));
                            Thread.sleep(sec * 1000);
                            totalRT = System.currentTimeMillis();
                        }
                    }
                    return;
                }

                if (args[0].equals("async")) {
                    if (!JToy.type.equals("a") && !JToy.type.equals("b")) {
                        logger.debug("Unable to set async behaviour to a correct server");
                        return;
                    }
                    if (args.length == 3) {
                        int sec = Integer.parseInt(args[1]);
                        int duration = Integer.parseInt(args[2]);
                        asyncPeriod(sec * 1000, duration * 1000);
                    }
                    return;
                }

                if (args[0].equals("byz")) {
//                    if (!JToy.type.equals("bs") && !JToy.type.equals("bf")) {
//                        logger.debug("Unable to set byzantine behaviour to non async server");
//                        return;
//                    }
                    setByzSetting(args);
                    return;
                }

                if (args[0].equals("res")) {
                    if (args.length == 3) {
                        if (!args[1].equals("-p")) return;
                        String path = args[2];
                        writeToScv(path);

                    }
                    return;
                }

                if (args[0].equals("sigTest")) {
                    logger.info("Accepted sigTest");
                    sigTets(outPath);
                    return;

                }
//                if (args[0].equals("bm")) {
//                    if (args.length != 7) return;
//                    if (!args[1].equals("-t")) return;
//                    if (!args[3].equals("-s")) return;
//                    if (!args[5].equals("-p")) return;
//                    runBenchMark(Integer.parseInt(args[2]), Integer.parseInt(args[4]), args[6]);
//                    return;
//                }

                if (args[0].equals("status")) {
                    if (args.length == 5) {
                        int channel = Integer.parseInt(args[1]);
                        int pid = Integer.parseInt(args[2]);
                        int bid = Integer.parseInt(args[3]);
                        int tid  = Integer.parseInt(args[4]);

                        String stat = txStatus(channel, pid, bid, tid);
                        System.out.println(format("[channel=%d ; pid:%d ; bid=%d ; tid=%d] status=%s", channel, pid,
                                bid, tid, stat));
                        return;
                    }
                }
                logger.error(format("Invalid command %s", Arrays.toString(args)));
//            } catch (Exception e) {
//                logger.error("", e);
//            }

        }
        private void help() {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Optimistic Total Ordering System ", options);
        }

        private void init() {
            JToy.s.start();
        }

        private void serve() {
            if (JToy.type.equals("m")) return;
            logger.debug("start serving");
            JToy.s.serve();
        }
        private void stop() {
//            JToy.s.stop();
            JToy.s.shutdown();
        }

//        private void latency() throws InterruptedException {
//            init();
//            serve();
//            while (true) {
//                Thread.sleep(10 * 60 * 1000);
//            }
//        }
        private Types.txID addtx(String data, int clientID) {
            return JToy.s.addTransaction(data.getBytes(), clientID);
        }

        private String txStatus(int channel, int pid, int bid, int tid) {
            Types.txID txid = Types.txID.newBuilder()
                    .setProposerID(pid)
                    .setBid(bid)
                    .setTxNum(tid)
                    .setChannel(channel)
                    .build();
            int stat = JToy.s.isTxPresent(txid);

            switch (stat) {
                case -1: return "Not exist";
                case 0: return "Approved";
//                case 1: return "Proposed";
//                case 2: return "Approved";
//                case 3: return "Pending";
            }
            return null;
        }
        private void signBlockFromBuilder(Types.Block.Builder b) {
            byte[] tHash = new byte[0];
            for (Types.Transaction t : b.getDataList()) {
                tHash = DigestMethod.hash(ArrayUtils.addAll(tHash, t.toByteArray()));
            }
            b.setHeader(b.getHeader().toBuilder().setProof(blockDigSig.sign(b.getHeader()))).build();
        }
        private Types.Block.Builder createBlock(int txSize) {
            Types.Block.Builder bb = Types.Block.newBuilder();
            for (int i = 0 ; i < Config.getMaxTransactionsInBlock() ; i++) {
                SecureRandom random = new SecureRandom();
                byte[] tx = new byte[txSize];
                random.nextBytes(tx);
                bb.addData(Types.Transaction.newBuilder()
                        .setId(Types.txID.newBuilder()
                                .setTxNum(0)
                                .setProposerID(0)
                                .setChannel(0))
                        .setClientID(0)
                        .setData(ByteString.copyFrom(tx))
                        .build());
            }

            SecureRandom random = new SecureRandom();
            byte[] tx = new byte[32];
            random.nextBytes(tx);

            return bb.setHeader(Types.BlockHeader.newBuilder()
                    .setM(Types.Meta.newBuilder()
                            .setChannel(0)
                            .setSender(0)
                            .setCidSeries(0)
                            .setCid(0).build())
                    .setHeight(0)
                    .setPrev(ByteString.copyFrom(tx)));


        }
        private void sigTets(String pathString) throws IOException, InterruptedException {
            logger.info(format("Starting sigTest [%d, %d]", Config.getTxSize(), Config.getMaxTransactionsInBlock()));
            ExecutorService executor = Executors.newFixedThreadPool(Config.getC());
            int bareTxSize = Types.Transaction.newBuilder()
                    .setClientID(0)
                    .setId(Types.txID.newBuilder()
                            .setTxNum(0)
                            .setProposerID(0)
                            .setChannel(0))
                    .setClientTs(System.currentTimeMillis())
                    .setServerTs(System.currentTimeMillis())
                    .build().getSerializedSize();
            int tSize = max(0, Config.getTxSize() - bareTxSize);

            CountDownLatch latch1 = new CountDownLatch(Config.getC());
            AtomicInteger avgSig = new AtomicInteger(0);
            AtomicBoolean stop = new AtomicBoolean(false);
            long start = System.currentTimeMillis();
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            for (int c = 0 ; c < Config.getC() ; c++) {
                int finalC = c;
                (executor).submit(() -> {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
                    int sigCount = 0;
                    Types.Block.Builder b = createBlock(tSize);
                    while (!stop.get()) {
                        signBlockFromBuilder(b);
                        sigCount++;
                    }
                    logger.info(format("finishing verForExec [%d] avg is [%d]", finalC, sigCount));
                    avgSig.addAndGet(sigCount);
                    latch1.countDown();
                });
            }
            logger.info(format("Await termination 1"));
            Thread.sleep(60 * 1000);
            stop.set(true);
//            executor.awaitTermination(120, TimeUnit.SECONDS);
            latch1.await();
            int total = (int) ((System.currentTimeMillis() - start) / 1000);
            executor.shutdownNow();
            logger.info(format("res [%d, %d]",  avgSig.get(), total));
            int sigPerSec = avgSig.get() / total;
            int verPerSec = 0; //avgVer.get() / Config.getC();
            Path path = Paths.get(pathString,   String.valueOf(0), "sig_summery.csv");
            File f = new File(path.toString());
            if (!f.exists()) {
                f.getParentFile().mkdirs();
                f.createNewFile();
            }
            logger.info(format("Collecting results"));
            FileWriter writer = new FileWriter(path.toString(), true);
            DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy-HH:mm:ss");
            List<String> row = Arrays.asList(dateFormat.format(new Date()),
                    String.valueOf(Config.getC()),
                    String.valueOf(bareTxSize + tSize),
                    String.valueOf(Config.getMaxTransactionsInBlock()),
                    String.valueOf(sigPerSec));
            CSVUtils.writeLine(writer, row);
            writer.flush();
            writer.close();
        }
        private void writeToScv(String pathString) {
            if (JToy.type.equals("m")) return;
            DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy-HH:mm:ss");
            Path path = Paths.get(pathString, String.valueOf(JToy.s.getID()), dateFormat.format(new Date()) + ".csv");
            try {
                File f = new File(path.toString());

                f.getParentFile().mkdirs();
                f.createNewFile();
                FileWriter writer = new FileWriter(path.toString());
                int nob = JToy.s.getBCSize();
                long fts = 0;
                long lts = 0;
                int tCount = 0;
                int txSize = -1;
                fts = JToy.s.nonBlockingDeliver(1).getSt().getDecided();
                lts = JToy.s.nonBlockingDeliver(nob - 1).getSt().getDecided();
                tCount = (nob - 1) * Config.getMaxTransactionsInBlock();
                txSize = JToy.s.nonBlockingDeliver(1).getData(0).getSerializedSize();
                for (int i = 0 ; i < nob ; i++) {
                    Types.Block b = JToy.s.nonBlockingDeliver(i);
                    for (Types.Transaction t : b.getDataList()) {
                        if (fts == 0) {
                            fts = b.getSt().getDecided();
                            lts = fts;
                        }
                        fts = min(fts, b.getSt().getDecided());
                        lts = max(lts, b.getSt().getDecided());
                        tCount++;
                        if (txSize == -1) {
                            txSize = t.getSerializedSize();
                        }
                        List<String> row = Arrays.asList(format("[%d:%d]", t.getId().getProposerID(), t.getId().getTxNum())
                                , String.valueOf(t.getSerializedSize()),
                                String.valueOf(t.getClientID()), String.valueOf(b.getSt().getDecided()),
                                String.valueOf(t.getData().size()), String.valueOf(i),
                                String.valueOf(b.getHeader().getM().getSender()));
                        CSVUtils.writeLine(writer, row);
                    }
                }
                writer.flush();
                writer.close();
//                writeSummery(pathString, tCount, txSize, fts, lts);
            } catch (IOException e) {
                logger.error("", e);
            }
        }

        boolean validateBC() {
            for (int i = 1 ; i < JToy.s.getBCSize() ; i++) {
                if (!BaseBlockchain.validateBlockHash(JToy.s.nonBlockingDeliver(i - 1),
                        JToy.s.nonBlockingDeliver(i))) {
                    System.out.println(String.format("Invalid Blockchain!! [%d -> %d]", i-1, i));
                    return false;
                }
            }
            return true;
        }
        void writeSummery(String pathString) {
            long avgWt = 0;
            int newTxCount = 0;
            int nob = JToy.s.getBCSize();
            for (int i = 0 ; i < nob ; i++) {
                Types.Block b = JToy.s.nonBlockingDeliver(i);
                for (Types.Transaction t : b.getDataList()) {
                    avgWt += b.getSt().getDecided() - Longs.fromByteArray(Arrays.copyOfRange(t.getData().toByteArray(), 0, 8));
                }
            }
            Path path = Paths.get(pathString,   String.valueOf(JToy.s.getID()), "summery.csv");
            File f = new File(path.toString());

            try {
                if (!f.exists()) {
                    f.getParentFile().mkdirs();
                    f.createNewFile();
                }
                FileWriter writer = null;
                writer = new FileWriter(path.toString(), true);
//                List<String> head = Arrays.asList("ts","id","type","channels","fm","txSize","txInBlock","txTotal"
//                        ,"duration","txPsec","blocksNum","avgTxInBlock","eRate","dRate");
//
//                CSVUtils.writeLine(writer, head);
                Statistics st = JToy.s.getStatistics();

                double time = ((double) st.lastTxTs - st.firstTxTs) / 1000;
//                double time = ((double) totalRT - st.firstTxTs) / 1000;
                int thrp = ((int) (st.txCount / time)); // / 1000;
                double opRate = ((double) st.optemisticDec) / ((double) st.totalDec);
                long delaysAvgMs = 0; //st.delaysSum / st.txCount;
                int avgTxInBlock = st.txCount / nob;
                double eRate = ((double)st.eb) / ((double) st.all);
                double dRate = ((double)st.deliveredTime) / ((double) st.all);
                DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy-HH:mm:ss");
                List<String> row = Arrays.asList(String.valueOf(validateBC()), dateFormat.format(new Date()), String.valueOf(JToy.s.getID()),
                        JToy.type, String.valueOf(Config.getC()), String.valueOf(Config.getTMO()), String.valueOf(Config.getFastMode()),
                        String.valueOf(st.txSize), String.valueOf(Config.getMaxTransactionsInBlock()),
                        String.valueOf(st.txCount), String.valueOf(time), String.valueOf(thrp),
//                        String.valueOf(st.totalDec), String.valueOf(st.optemisticDec), String.valueOf(opRate),
                        String.valueOf(nob), String.valueOf(avgTxInBlock),String.valueOf(delaysAvgMs), String.valueOf(opRate),
                        String.valueOf(eRate), String.valueOf(dRate), String.valueOf(st.syncEvents));
                CSVUtils.writeLine(writer, row);
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    void writeBlocksStatisticsSummery(String pathString)  {
        Path path = Paths.get(pathString,   String.valueOf(JToy.s.getID()), "blocksStatSummery.csv");
        try {
            File f = new File(path.toString());
            if (!f.exists()) {
                f.getParentFile().mkdirs();
                f.createNewFile();
            }
            FileWriter writer = null;
            writer = new FileWriter(path.toString(), true);
            int nob = JToy.s.getBCSize();
//            DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy-HH:mm:ss");
            Statistics st = JToy.s.getStatistics();
//            long total = 0;
            long signaturePeriod = 0;
//            long create2propose = 0;
            long verificationPeriod = 0;
            long propose2tentative = 0;
            long tentative2permanent = 0;
            long channelPermanent2decide = 0;
            long propose2permanentchannel= 0;
            long propose2decide = 0;
            for (int i = 1 ; i < nob ; i++) {
                Types.Block b = JToy.s.nonBlockingDeliver(i);
                Types.blockStatistics bst = b.getSt();
                signaturePeriod += bst.getSign();
//                create2propose += bst.getProposed() - bst.getCreated();
                verificationPeriod += bst.getVerified();
                propose2tentative += bst.getChannelDecided() - bst.getProposed();
                tentative2permanent += bst.getPd() - bst.getChannelDecided();
                channelPermanent2decide += bst.getDecided() - bst.getPd();
                propose2permanentchannel += bst.getPd() - bst.getProposed();
                propose2decide += bst.getDecided() - bst.getProposed();
//                total += b.getSt().getDecided() - b.getSt().getProposed();
            }
//            long avgTotal = total / nob;
            signaturePeriod = signaturePeriod / nob;
//            create2propose = create2propose / nob;
            verificationPeriod = verificationPeriod / nob;
            propose2tentative = propose2tentative / nob;
            tentative2permanent = tentative2permanent / nob;
            channelPermanent2decide = channelPermanent2decide / nob;
            propose2permanentchannel = propose2permanentchannel / nob;
            propose2decide = propose2decide / nob;
            List<String> row = Arrays.asList(String.valueOf(JToy.s.getID()),
                    JToy.type, String.valueOf(Config.getC()), String.valueOf(st.txSize),
                    String.valueOf(Config.getMaxTransactionsInBlock()),
                    String.valueOf(signaturePeriod),
                    String.valueOf(verificationPeriod),
//                    String.valueOf(create2propose),
                    String.valueOf(propose2tentative),
                    String.valueOf(tentative2permanent),
                    String.valueOf(channelPermanent2decide),
                    String.valueOf(propose2permanentchannel),
                    String.valueOf(propose2decide));
//                    String.valueOf(avgTotal));
            CSVUtils.writeLine(writer, row);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

        void writeBlocksStatistics(String pathString)  {
            Path path = Paths.get(pathString,   String.valueOf(JToy.s.getID()), "blocksStat.csv");
            try {
                File f = new File(path.toString());
                if (!f.exists()) {
                    f.getParentFile().mkdirs();
                    f.createNewFile();
                }
                FileWriter writer = null;
                writer = new FileWriter(path.toString(), true);
                int nob = JToy.s.getBCSize();
//                DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy-HH:mm:ss");
                Statistics st = JToy.s.getStatistics();
                for (int i = 1 ; i < nob ; i++) {
                    Types.Block b = JToy.s.nonBlockingDeliver(i);
//                    logger.info(format("[height=%d] [tentative=%d] [permanent=%d] [diff=%d]",
//                            b.getHeader().getHeight(), b.getSt().getChannelDecided(),
//                            b.getSt().getPd(), b.getSt().getPd() - b.getSt().getChannelDecided()));
                    Types.blockStatistics bst = b.getSt();
//                    long total = bst.getDecided() - bst.getChannelDecided();
                    long signaturePeriod = bst.getSign();
                    long verificationPeriod = bst.getVerified();
//                    long create2propose = bst.getProposed() - bst.getCreated();
                    long propose2tentative = bst.getChannelDecided() - bst.getProposed();
                    long tentative2permanent = bst.getPd() - bst.getChannelDecided();
                    long channelPermanent2decide = bst.getDecided() - bst.getPd();
                    long propose2permanentchannel= bst.getPd() - bst.getProposed();
                    long propose2decide = bst.getDecided() - bst.getProposed();
                    List<String> row = Arrays.asList(String.valueOf(JToy.s.getID()),
                            JToy.type, String.valueOf(Config.getC()), String.valueOf(st.txSize),
                            String.valueOf(Config.getMaxTransactionsInBlock()),
                            String.valueOf(b.getDataCount()),
                            String.valueOf(b.getHeader().getHeight()),
                            String.valueOf(signaturePeriod),
                            String.valueOf(verificationPeriod),
//                            String.valueOf(create2propose),
                            String.valueOf(propose2tentative),
                            String.valueOf(tentative2permanent),
                            String.valueOf(channelPermanent2decide),
                            String.valueOf(propose2permanentchannel),
                            String.valueOf(propose2decide));
//                            String.valueOf(total));
                    CSVUtils.writeLine(writer, row);
                }
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
//        private void runBenchMark(int tSize, int tNumber, String csvPath) throws InterruptedException {
//
//            Thread.sleep(15 * 1000);
//            logger.info(format("[#%d] start serving...", JToy.s.getID()));
//            serve();
////            Thread.sleep(10 * 1000);
////            loadServer(tSize);
//            Thread.sleep(60 * 1 * 1000);
//            JToy.s.shutdown();
////            writeToScv(csvPath);
//            writeSummery(csvPath);
//
////            System.exit(0);
//        }
//        private void loadServer(int tSize) throws InterruptedException {
//            Random rand = new Random();
//            long tts = System.currentTimeMillis();
//            int bareTxSize = Types.Transaction.newBuilder()
//                    .setClientID(0)
//                    .setId(Types.txID.newBuilder().setTxID(UUID.randomUUID().toString()).build())
//                    .setClientTs(tts)
//                    .setServerTs(tts)
//                    .build().getSerializedSize();
//            int txSize = max(0, tSize - bareTxSize);
//            AtomicBoolean stopped = new AtomicBoolean(false);
//            int clients = 2;
//            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(clients);
//            for (int i = 0 ; i < clients ;i++) {
//                int finalI = i;
//                executor.submit( () -> {
//                    while (!stopped.get()) {
//                        int cID = rand.nextInt(finalI);
////                        byte[] ts = Longs.toByteArray(System.currentTimeMillis());
//                        SecureRandom random = new SecureRandom();
//                        byte[] tx = new byte[txSize];
//                        random.nextBytes(tx);
//                        JToy.s.addTransaction(tx, cID);
//                    }
//                });
//            }
//            Thread.sleep(60 * 1* 1000);
//            stopped.set(true);
//            Thread.sleep(2* 1000);
//            executor.shutdownNow();
//        }
        private void setByzSetting(String[] args) {
            System.out.println("Setting byz");
            boolean fullByz = Integer.parseInt(args[1]) == 1;
            List<List<Integer>> groups = new ArrayList<>();
            int i = 2;
            while (i < args.length) {
                i++;
                List<Integer> group = new ArrayList<>();
                while (i < args.length && !args[i].equals("-g")) {
                    group.add(Integer.parseInt(args[i]));
                    i++;
                }
                groups.add(group);
            }
            JToy.s.setByzSetting(fullByz, groups);
        }

        private void asyncPeriod(int sec, int duration) throws InterruptedException {
            System.out.println(format("setting async params [%d] sec delay for [%d] sec", sec/1000, duration/1000));
            JToy.s.setAsyncParam(sec);
            if (duration > 0) {
                Thread.sleep(duration);
            }
            System.out.println(format("return to normal"));
            JToy.s.setAsyncParam(0);
        }

}
