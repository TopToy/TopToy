package app;
import blockchain.data.BCS;
import com.google.protobuf.ByteString;
import config.Config;
import crypto.BlockDigSig;
import das.ms.BFD;
import org.apache.commons.cli.*;
import proto.Types;
import utils.statistics.Statistics;
import utils.CSVUtils;
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
import java.util.stream.Collectors;

import static crypto.BlockDigSig.hashBlockData;
import static java.lang.String.format;

class Cli {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Cli.class);
        private Options options = new Options();
        private static String outPath = "/tmp/JToy/res/";
        private AtomicBoolean recorded = new AtomicBoolean(false);

        Cli() {
            options.addOption("help", "print this message");
            options.addOption("init", "init a toy server");
            options.addOption("serve", "start the server");
            options.addOption("stop", "terminate the server");
            options.addOption("quit", "exit Toy terminal");

            options.addOption(Option.builder("wait")
                    .hasArg()
                    .desc("Usage: wait [sec]\n" +
                            "waits for [sec] second")
                    .build());

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (!recorded.get()) {
                    Statistics.deactivate();
                    if (Statistics.getSyncs() == 0) {
                        writeSummery(outPath);
                    } else {
                        writeByzSummery(outPath);
                    }
//                    writeBlocks();
                }
//
            }));
        }


        void parse(String[] args) throws InterruptedException, IOException {

            if (args[0].equals("help")) {
                help();
                return;
            }
            if (args[0].equals("init")) {
                init();
                System.out.println("Init server... [OK]");
                return;
            }


            if (args[0].equals("serve")) {
                serve();
                System.out.println("Serving... [OK]");
                return;
            }

            if (args[0].equals("stop")) {
                stop();
                System.out.println("stop server... [OK]");
                return;
            }

            if (args[0].equals("quit")) {
                if (Statistics.getSyncs() == 0) {
                    writeSummery(outPath);
                } else {
                    writeByzSummery(outPath);
                }

//                writeBlocks();
                recorded.set(true);
                System.exit(0);
            }

            if (args[0].equals("wait")) {
                if (args.length == 2) {
                    int sec = Integer.parseInt(args[1]);
                    if (sec > 0) {

                        System.out.println(format("waits for %d seconds ", sec));
                        Thread.sleep(sec * 1000);
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
                JToy.s.setByzSetting();
                return;
            }

//            if (args[0].equals("sigTest")) {
//                logger.info("Accepted sigTest");
//                int time = Integer.parseInt(args[1]);
//                sigTest(outPath, time);
//                return;
//
//            }

            if (args[0].equals("stStart")) {
               Statistics.activate(JToy.s.getID());
                return;

            }

            if (args[0].equals("stStop")) {
                Statistics.deactivate();
                return;

            }

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

        }
        private void help() {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Optimistic Total Ordering System ", options);
        }

        private void init() {
            JToy.init();
            JToy.s.start();
        }

        private void serve() {
            if (JToy.type.equals("m")) return;
            logger.debug("start serving");
            JToy.s.serve();
        }
        private void stop() {
            JToy.s.shutdown();
        }


        private String txStatus(int channel, int pid, int bid, int tid) throws InterruptedException {
            Types.txID txid = Types.txID.newBuilder()
                    .setProposerID(pid)
                    .setBid(bid)
                    .setTxNum(tid)
                    .setChannel(channel)
                    .build();
            int stat = JToy.s.status(txid, false);

            switch (stat) {
                case -1: return "Not exist";
                case 0: return "Approved";
            }
            return null;
        }
        private void signBlockFromBuilder(Types.Block.Builder b) {
            Types.Block.Builder b1 = b.setHeader(b.getHeader()
                    .toBuilder()
                    .setTransactionHash(ByteString.copyFrom(hashBlockData(b.build()))));
            b1.setHeader(b.getHeader().toBuilder()
                    .setProof(BlockDigSig.sign(b1.getHeader()))).build();
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
                                .setChannel(0)
                                .setBid(0))
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
                            .setCidSeries(0)
                            .setCid(0))
                    .setHeight(0)
                    .setBid(Types.BlockID.newBuilder().setBid(0).setPid(0).build())
                    .setPrev(ByteString.copyFrom(tx)));


        }
        private void sigTest(String pathString, int time) throws IOException, InterruptedException {
            logger.info(format("Starting sigTest [%d, %d]", Config.getTxSize(), Config.getMaxTransactionsInBlock()));
            ExecutorService executor = Executors.newFixedThreadPool(Config.getC());
            CountDownLatch latch1 = new CountDownLatch(Config.getC());
            AtomicInteger avgSig = new AtomicInteger(0);
            AtomicBoolean stop = new AtomicBoolean(false);
            for (int c = 0 ; c < Config.getC() ; c++) {
                int finalC = c;
                (executor).submit(() -> {
                    int sigCount = 0;
                    Types.Block.Builder b = createBlock(Config.getTxSize());
                    while (!stop.get()) {
                        signBlockFromBuilder(b);
                        sigCount++;
                    }
                    logger.info(format("finishing verForExec [%d] avg is [%d]", finalC, sigCount));
                    avgSig.addAndGet(sigCount);
                    latch1.countDown();
                });
            }
            logger.info("Await termination 1");
            Thread.sleep(time * 1000);
            stop.set(true);
//            executor.awaitTermination(120, TimeUnit.SECONDS);
            latch1.await();
            executor.shutdownNow();
            int sigPerSec = avgSig.get() / time;
            Path path = Paths.get(pathString,   String.valueOf(0), "sig_summery.csv");
            File f = new File(path.toString());
            if (!f.exists()) {
                f.getParentFile().mkdirs();
                f.createNewFile();
            }
            logger.info("Collecting results");
            FileWriter writer = new FileWriter(path.toString(), true);
            DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy-HH:mm:ss");
            List<String> row = Arrays.asList(
                    String.valueOf(Config.getC())
                    , String.valueOf(time)
                    , String.valueOf(Config.getTxSize())
                    , String.valueOf(Config.getMaxTransactionsInBlock())
                    , String.valueOf(sigPerSec)
            );
            CSVUtils.writeLine(writer, row);
            writer.flush();
            writer.close();
        }

        private void writeBlocks() {

            if (Config.getTxSize() != 512) return;
            logger.info("Starting writeBlocks");
            String pathString = "/tmp/JToy/res/";
            Path path = Paths.get(pathString, String.valueOf(JToy.s.getID()), "bsummery.csv");
            File f = new File(path.toString());

            try {
                if (!f.exists()) {
                    f.getParentFile().mkdirs();
                    f.createNewFile();
                }
                FileWriter writer;
                writer = new FileWriter(path.toString(), true);
                List<List<String>> rows = new ArrayList<>();
                int workers = Config.getC();
                for (int i = Statistics.getH1() ; i < Statistics.getH2() ; i++) {
                    List<Types.Block> rBlocks = new ArrayList<>();
                    for (int  j = 0 ; j < workers ; j++) {
                        rBlocks.add(BCS.nbGetBlock(j, i));

                    }
                    long dlt = Collections.max(rBlocks.stream().map(b ->
                            b.getHeader().getHst().getDefiniteTime()).collect(Collectors.toList()));
                    for (int j = 0 ; j < workers ; j++) {
                        Types.Block b = rBlocks.get(j);
                        if (b.getId().getPid() != JToy.s.getID()) continue;
                        long pt = b.getBst().getProposeTime();
                        long tt = b.getHeader().getHst().getTentativeTime();
                        long dt = b.getHeader().getHst().getDefiniteTime();
                        rows.add(Arrays.asList(
                                    String.valueOf(Config.getMaxTransactionsInBlock())
                                    , String.valueOf(Config.getTxSize())
                                    , String.valueOf(workers)
                                    , String.valueOf(j)
                                    , String.valueOf(b.getId().getPid())
                                    , String.valueOf(b.getId().getBid())
                                    , String.valueOf(b.getHeader().getHeight())
                                    , String.valueOf(b.getDataCount())
                                    , String.valueOf(tt - pt)
                                    , String.valueOf(dt - pt)
                                    , String.valueOf(dlt - pt)
                                    , String.valueOf(dt - tt)
                                    , String.valueOf(dlt - dt)
                                )

                        );
                    }
                }
                CSVUtils.writeLines(writer, rows);
                writer.flush();
                writer.close();
                logger.info("ended writeBlocks");
            } catch (Exception e) {
                logger.error(e);
            }
        }

        private void writeSummery(String pathString) {
            logger.info("Starting writeSummery");
            Path path = Paths.get(pathString, String.valueOf(JToy.s.getID()), "summery.csv");
            File f = new File(path.toString());

            try {
                if (!f.exists()) {
                    f.getParentFile().mkdirs();
                    f.createNewFile();
                }
                FileWriter writer;
                writer = new FileWriter(path.toString(), true);

                int nob = Statistics.getNob();
                int noeb = Statistics.getNeb();
                int txCount = Statistics.getTxCount();
                int stBlockNum = Statistics.getStBlockNum();
                long txSize = Config.getTxSize();
                int txInBlock = Config.getMaxTransactionsInBlock();
                int avgTxInBlock = 0;
                double acBP2T = Statistics.getAcBP2T();
                double acBP2D = Statistics.getAcBP2D();
                double acBP2DL = Statistics.getAcBP2DL();

                double acHP2T = Statistics.getAcHP2T();
                double acHP2D = Statistics.getAcHP2D();
                double acHT2D = Statistics.getAcHT2D();
                double acHP2DL = Statistics.getAcHP2DL();
                double acHD2DL = Statistics.getAcHD2DL();

                long time = (Statistics.getStop() - Statistics.getStart()) / 1000;
                if (nob > 0) {
                    avgTxInBlock = txCount / nob;

                }

                double BP2T = 0;
                double BP2D = 0;
                double BP2DL = 0;

                double HP2T = 0;
                double HP2D = 0;
                double HT2D = 0;
                double HP2DL = 0;
                double HD2DL = 0;

                if (stBlockNum > 0) {
                    BP2T = acBP2T / stBlockNum;
                    BP2D = acBP2D / stBlockNum;
                    BP2DL = acBP2DL / stBlockNum;

                    HP2T = acHP2T / stBlockNum;
                    HP2D = acHP2D / stBlockNum;
                    HT2D = acHT2D / stBlockNum;
                    HP2DL = acHP2DL / stBlockNum;
                    HD2DL = acHD2DL / stBlockNum;
                }

                int tps = 0;
                int bps = 0;
                if (time > 0) {
                    tps = ((int) (txCount / time));
                    bps = (int) (nob / time);
                }

                int pos = Statistics.getPos();
                int neg = Statistics.getNeg();
                int opt = Statistics.getOpt();
                int all = Statistics.getAll();

                double opRate = 0;
                double posRate = 0;
                double negRate = 0;
                double avgNegDecTime = 0;
                long avgTmo = 0;
                long avgActTmo = 0;

                if (pos > 0) {
                    opRate = ((double) opt) / ((double) pos);
                }
                if (all > 0) {
                    posRate = ((double) pos) / ((double) all);
                    negRate = ((double) neg) / ((double) all);
                    avgTmo = Statistics.getTmo() / all;
                    avgActTmo = Statistics.getActTmo() / all;
                }
                if (neg > 0) {

                    avgNegDecTime = ((double) Statistics.getNegTime() / (double) neg);
                }
                int syncEvents = Statistics.getSyncs();
                boolean valid = true; //BCS.isValid();

                List<String> row = Arrays.asList(
                        String.valueOf(valid)
                        , String.valueOf(JToy.s.getID())
                        , JToy.type
                        , String.valueOf(Config.getC())
                        , String.valueOf(avgTmo)
                        , String.valueOf(avgActTmo)
                        , String.valueOf(Statistics.getMaxTmo())
                        , String.valueOf(txSize)
                        , String.valueOf(txInBlock)
                        , String.valueOf(txCount)
                        , String.valueOf(time)
                        , String.valueOf(tps)
                        , String.valueOf(nob)
                        , String.valueOf(noeb)
                        , String.valueOf(bps)
                        , String.valueOf(avgTxInBlock)
                        , String.valueOf(opt)
                        , String.valueOf(opRate)
                        , String.valueOf(pos)
                        , String.valueOf(posRate)
                        , String.valueOf(neg)
                        , String.valueOf(negRate)
                        , String.valueOf(avgNegDecTime)
                        , String.valueOf(syncEvents)
                        , String.valueOf(BP2T)
                        , String.valueOf(BP2D)
                        , String.valueOf(BP2DL)
                        , String.valueOf(HP2T)
                        , String.valueOf(HP2D)
                        , String.valueOf(HP2DL)
                        , String.valueOf(HT2D)
                        , String.valueOf(HD2DL)
                        , String.valueOf(BFD.getSuspected())
                );
                CSVUtils.writeLine(writer, row);
                writer.flush();
                writer.close();
                logger.info("ended writeSummery");
            } catch (IOException e) {
                logger.error(e);
            }

        }

    void writeByzSummery(String pathString) {
        logger.info("Starting writeSummery");
        Path path = Paths.get(pathString, String.valueOf(JToy.s.getID()), "summery.csv");
        File f = new File(path.toString());

        try {
            if (!f.exists()) {
                f.getParentFile().mkdirs();
                f.createNewFile();
            }
            FileWriter writer;
            writer = new FileWriter(path.toString(), true);
            int workers = Config.getC();

            int nob = 0;
            int noeb = 0;
            int txCount = 0;
            for (int i = Statistics.getH1() ; i < Statistics.getH2() ; i++) {
                for (int j = 0 ; j < workers ; j++) {
                    Types.Block b = BCS.nbGetBlock(j, i);
                        nob++;
                        if (b.getDataCount() == 0) {
                            noeb++;
                        }
                        txCount += b.getDataCount();

                }
            }

            long txSize = Config.getTxSize();
            int txInBlock = Config.getMaxTransactionsInBlock();
            int avgTxInBlock = 0;

            long time = (Statistics.getStop() - Statistics.getStart()) / 1000;
            if (nob > 0) {
                avgTxInBlock = txCount / nob;

            }

            int tps = 0;
            int bps = 0;
            if (time > 0) {
                tps = ((int) (txCount / time));
                bps = (int) (nob / time);
            }

            int pos = Statistics.getPos();
            int neg = Statistics.getNeg();
            int opt = Statistics.getOpt();
            int all = Statistics.getAll();

            double opRate = 0;
            double posRate = 0;
            double negRate = 0;
            double avgNegDecTime = 0;
            long avgTmo = 0;
            long avgActTmo = 0;

            if (pos > 0) {
                opRate = ((double) opt) / ((double) pos);
            }
            if (all > 0) {
                posRate = ((double) pos) / ((double) all);
                negRate = ((double) neg) / ((double) all);
                avgTmo = Statistics.getTmo() / all;
                avgActTmo = Statistics.getActTmo() / all;
            }
            if (neg > 0) {

                avgNegDecTime = ((double) Statistics.getNegTime() / (double) neg);
            }
            int syncEvents = Statistics.getSyncs();
            boolean valid = true; //BCS.isValid();

            List<String> row = Arrays.asList(
                    String.valueOf(valid)
                    , String.valueOf(JToy.s.getID())
                    , JToy.type
                    , String.valueOf(Config.getC())
                    , String.valueOf(avgTmo)
                    , String.valueOf(avgActTmo)
                    , String.valueOf(Statistics.getMaxTmo())
                    , String.valueOf(txSize)
                    , String.valueOf(txInBlock)
                    , String.valueOf(txCount)
                    , String.valueOf(time)
                    , String.valueOf(tps)
                    , String.valueOf(nob)
                    , String.valueOf(noeb)
                    , String.valueOf(bps)
                    , String.valueOf(avgTxInBlock)
                    , String.valueOf(opt)
                    , String.valueOf(opRate)
                    , String.valueOf(pos)
                    , String.valueOf(posRate)
                    , String.valueOf(neg)
                    , String.valueOf(negRate)
                    , String.valueOf(avgNegDecTime)
                    , String.valueOf(syncEvents)
                    , String.valueOf(0)
                    , String.valueOf(0)
                    , String.valueOf(0)
                    , String.valueOf(0)
                    , String.valueOf(0)
                    , String.valueOf(0)
                    , String.valueOf(0)
                    , String.valueOf(0)
                    , String.valueOf(BFD.getSuspected())
            );
            CSVUtils.writeLine(writer, row);
            writer.flush();
            writer.close();
            logger.info("ended writeSummery");
        } catch (IOException e) {
            logger.error(e);
        }

    }


    private void asyncPeriod(int sec, int duration) throws InterruptedException {
        System.out.println(format("setting async params [%d] sec delay for [%d] sec", sec/1000, duration/1000));
        JToy.s.setAsyncParam(sec);
        if (duration > 0) {
            Thread.sleep(duration);
        }
        System.out.println("return to normal");
        JToy.s.setAsyncParam(0);
    }

}
