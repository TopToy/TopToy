package app;
import com.google.protobuf.ByteString;
import config.Config;
//import crypto.rmfDigSig;
import crypto.DigestMethod;
import crypto.blockDigSig;
import org.apache.commons.cli.*;
import org.apache.commons.lang.ArrayUtils;
import proto.Types;
import utils.CSVUtils;

import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Math.min;
import static java.lang.StrictMath.max;
import static java.lang.String.format;
import static java.lang.String.valueOf;

public class cli {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(cli.class);
        private Options options = new Options();

        public cli() {
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
        }


        void parse(String[] args) {
//            CommandLineParser parser = new DefaultParser();
            try {
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
                    System.exit(0);
                    return;
                }

                if (args[0].equals("wait")) {
                    if (args.length == 2) {
                        int sec = Integer.parseInt(args[1]);
                        Thread.sleep(sec * 1000);

                    }
                    return;
                }

                if (args[0].equals("async")) {
                    if (!JToy.type.equals("a")) {
                        logger.debug("Unable to set async behaviour to non async server");
                        return;
                    }
                    if (args.length == 2) {
                        int sec = Integer.parseInt(args[1]);
                        JToy.s.setAsyncParam(sec);
                    }
                    return;
                }

                if (args[0].equals("byz")) {
                    if (!JToy.type.equals("bs") && !JToy.type.equals("bf")) {
                        logger.debug("Unable to set byzantine behaviour to non async server");
                        return;
                    }
                    setByzSetting(args);
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
                    if (args.length == 7) {
                        if (!args[1].equals("-t")) return;
                        if (!args[3].equals("-s")) return;
                        if (!args[5].equals("-p")) return;
                        String path = args[6];
                        int txSize = Integer.parseInt(args[4]);
                        int txNum = Integer.parseInt(args[2]);
                        sigTets(txNum, txSize, path);

                    }
                    return;

                }
                if (args[0].equals("bm")) {
                    if (args.length != 7) return;
                    if (!args[1].equals("-t")) return;
                    if (!args[3].equals("-s")) return;
                    if (!args[5].equals("-p")) return;
                    runBenchMark(Integer.parseInt(args[2]), Integer.parseInt(args[4]), args[6]);
                    return;
                }

                if (args[0].equals("tx")) {
                    if (args.length >= 3) {
                        String cmd = args[1];
                        if (cmd.equals("-a")) {
                            if (args.length == 4) {
                                String tx = args[2].replaceAll("\"", "");
                                int cID = Integer.parseInt(args[3]);
                                String txID = addtx(tx, cID);
                                System.out.println(format("txID=%s", txID));
                                return;
                            }
                        }
                        if (cmd.equals("-s")) {
                            if (args.length == 3) {
                                String txID = args[2];
                                String stat = txStatus(txID);
                                System.out.println(format("txID=%s status=%s", txID, stat));
                                return;
                            }
                        }
                    }


                }
                System.out.println(format("Invalid command %s", Arrays.toString(args)));
            } catch (Exception e) {
                logger.error("", e);
            }

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
            JToy.s.stop();
            JToy.s.shutdown();
        }


        private String addtx(String data, int clientID) {
            return JToy.s.addTransaction(data.getBytes(), clientID);
        }

        private String txStatus(String txID) {
            int stat = JToy.s.isTxPresent(txID);

            switch (stat) {
                case -1: return "Not exist";
                case 0: return "Waiting";
                case 1: return "Proposed";
                case 2: return "Approved";
                case 3: return "Pending";
            }
            return null;
        }

        private void sigTets(int txNum, int txSize, String pathString) throws IOException {


            Types.Block.Builder bb = Types.Block.newBuilder();
            for (int i = 0 ; i < txNum ; i++) {
                SecureRandom random = new SecureRandom();
                byte[] tx = new byte[txSize];
                random.nextBytes(tx);
                bb.addData(Types.Transaction.newBuilder()
                        .setTxID(UUID.randomUUID().toString())
                        .setClientID(0)
                        .setData(ByteString.copyFrom(tx))
                        .build());
            }
            byte[] tHash = new byte[0];
            for (Types.Transaction t : bb.getDataList()) {
                tHash = DigestMethod.hash(ArrayUtils.addAll(tHash, t.toByteArray()));
            }
            SecureRandom random = new SecureRandom();
            byte[] tx = new byte[8];
            random.nextBytes(tx);

            bb = bb.setHeader(Types.BlockHeader.newBuilder()
                    .setM(Types.Meta.newBuilder()
                            .setChannel(0)
                            .setSender(0)
                            .setCidSeries(0)
                            .setCid(0).build())
                    .setHeight(0)
                    .setPrev(ByteString.copyFrom(tx))
                    .setTransactionHash(ByteString.copyFrom(tHash))
                    .build());
            Types.Block b = bb.setHeader(bb.getHeader().toBuilder().setProof(blockDigSig.sign(bb.getHeader()))).build();
//            Types.Data.Builder d = Types.Data.newBuilder()
//                    .setMeta(Types.Meta.newBuilder()
//                            .setCid(0)
//                            .setCidSeries(0)
//                            .setSender(JToy.server.getID()))
//                    .setData(ByteString.copyFrom(tx));
//            d.setSig(rmfDigSig.sign(d));
//            Types.Data db = d.build();
            if (!blockDigSig.verify(JToy.s.getID(), b)) {
                return;
            }
            int sigCount = 0;
            long t = System.currentTimeMillis();
            while (new Timestamp(System.currentTimeMillis()).getTime() - t < 10 * 1000) {
                blockDigSig.sign(bb.getHeader());
                sigCount++;
            }
            sigCount = sigCount / 10;
            int verCount = 0;
            t = System.currentTimeMillis();
            while (System.currentTimeMillis() - t < 10 * 1000) {
                blockDigSig.verify(JToy.s.getID(), b);
                verCount++;
            }

            Path path = Paths.get(pathString,   String.valueOf(JToy.s.getID()), "sig_summery.csv");
            File f = new File(path.toString());
            if (!f.exists()) {
                f.getParentFile().mkdirs();
                f.createNewFile();
            }
            FileWriter writer = new FileWriter(path.toString(), true);
            DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy-HH:mm:ss");
            List<String> row = Arrays.asList(dateFormat.format(new Date()),
                    String.valueOf(JToy.s.getID()), String.valueOf(sigCount), String.valueOf(verCount));
            CSVUtils.writeLine(writer, row);
            writer.flush();
            writer.close();
        }
        private void writeToScv(String pathString) {
            if (JToy.type.equals("m")) return;
            DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy-HH:mm:ss");
            Path path = Paths.get(pathString, String.valueOf(JToy.s.getID()), dateFormat.format(new Date()) + ".csv");
            try {
//                File f = new File(path.toString());
//
//                f.getParentFile().mkdirs();
//                f.createNewFile();
//                FileWriter writer = new FileWriter(path.toString());
                int nob = JToy.s.getBCSize();
                long fts = 0;
                long lts = 0;
                int tCount = 0;
                int txSize = -1;
                fts = JToy.s.nonBlockingDeliver(1).getTs();
                lts = JToy.s.nonBlockingDeliver(nob - 1).getTs();
                tCount = (nob - 1) * Config.getMaxTransactionsInBlock();
                txSize = JToy.s.nonBlockingDeliver(1).getData(0).getSerializedSize();
//                for (int i = 0 ; i < nob ; i++) {
//                    Types.Block b = JToy.s.nonBlockingDeliver(i);
//                    for (Types.Transaction t : b.getDataList()) {
//                        if (fts == 0) {
//                            fts = b.getTs();
//                            lts = fts;
//                        }
//                        fts = min(fts, b.getTs());
//                        lts = max(lts, b.getTs());
//                        tCount++;
//                        if (txSize == -1) {
//                            txSize = t.getSerializedSize();
//                        }
//                        List<String> row = Arrays.asList(t.getTxID(),
//                                String.valueOf(t.getClientID()), String.valueOf(b.getTs()),
//                                String.valueOf(t.getData().size()), String.valueOf(i),
//                                String.valueOf(b.getHeader().getM().getSender()));
////                        CSVUtils.writeLine(writer, row);
//                    }
//                }
//                writer.flush();
//                writer.close();
                writeSummery(pathString, tCount, txSize, fts, lts);
            } catch (IOException e) {
                logger.error("", e);
            }
        }

        void writeSummery(String pathString, int tCount, int txSize, long fts, long lts) throws IOException {
            Path path = Paths.get(pathString,   String.valueOf(JToy.s.getID()), "summery.csv");
            File f = new File(path.toString());
            if (!f.exists()) {
                f.getParentFile().mkdirs();
                f.createNewFile();
            }
            FileWriter writer = new FileWriter(path.toString(), true);
            double time = ((double) lts - (double) fts) / 1000;
            int thrp = (int) (tCount / time);
            DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy-HH:mm:ss");
            List<String> row = Arrays.asList(dateFormat.format(new Date()), String.valueOf(JToy.s.getID()),
                    JToy.type, String.valueOf(Config.getC()), String.valueOf(Config.getFastMode()),
                    String.valueOf(txSize), String.valueOf(Config.getMaxTransactionsInBlock()), String.valueOf(tCount),
                    String.valueOf(time), String.valueOf(thrp));
            CSVUtils.writeLine(writer, row);
            writer.flush();
            writer.close();
        }
        private void runBenchMark(int tSize, int tNumber, String csvPath) throws InterruptedException {
            Random rand = new Random();
            String last = "";
            for (int i = 0 ; i < tNumber ; i++) {
                int cID = rand.nextInt( 100 );
                SecureRandom random = new SecureRandom();
                byte[] tx = new byte[tSize];
                random.nextBytes(tx);
                last = JToy.s.addTransaction(tx, cID);
            }
            Thread.sleep(30 * 1000);
            logger.info(format("[#%d] start serving...", JToy.s.getID()));
            serve();
//            while ((!JToy.type.equals("m")) && (!last.equals("")) && JToy.server.isTxPresent(last) != 2) {
//                Thread.sleep(5 * 1000);
//            }
//            if (JToy.type.equals("m")) {
//                Thread.sleep(60 * 1000);
//            }
            Thread.sleep(2 * 1000);
            AtomicBoolean stopped = new AtomicBoolean(false);
            long start = System.currentTimeMillis();
//            Object lock = new Object();
            Thread t = new Thread(() -> {
                while (!stopped.get()) {
                    int cID = rand.nextInt( 100 );
                    SecureRandom random = new SecureRandom();
                    byte[] tx = new byte[tSize];
                    random.nextBytes(tx);
                    JToy.s.addTransaction(tx, cID);
                }
            });
            t.start();
            Thread.sleep(60 * 1 * 1000);
            stopped.set(true);
            t.interrupt();
            t.join();
            JToy.s.stop();


//            Thread.sleep( 2 * 60 * 1000);

//            Thread t = new Thread(() -> );
//            t.start();
//            Thread.sleep(10 * 1000);
            writeToScv(csvPath);
            JToy.s.shutdown();
//            System.exit(0);
        }

        private void setByzSetting(String[] args) {
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

}
