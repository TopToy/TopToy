package app;
import blockchain.asyncBcServer;
import blockchain.byzantineBcServer;
import org.apache.commons.cli.*;
import proto.Types;
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

import static java.lang.String.format;

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
                        ((asyncBcServer) JToy.server).setAsyncParam(sec);
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
            JToy.server.start();
        }

        private void serve() {
            if (JToy.type.equals("m")) return;
            logger.debug("start serving");
            JToy.server.serve();
        }
        private void stop() {
            JToy.server.shutdown();
        }


        private String addtx(String data, int clientID) {
            return JToy.server.addTransaction(data.getBytes(), clientID);
        }

        private String txStatus(String txID) {
            int stat = JToy.server.isTxPresent(txID);

            switch (stat) {
                case -1: return "Not exist";
                case 0: return "Waiting";
                case 1: return "Proposed";
                case 2: return "Approved";
                case 3: return "Pending";
            }
            return null;
        }

        private void writeToScv(String pathString) {
            if (JToy.type.equals("m")) return;
            DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyyHH:mm:ss");
            Path path = Paths.get(pathString, String.valueOf(JToy.server.getID()), dateFormat.format(new Date()) + ".csv");
            try {
                File f = new File(path.toString());

                f.getParentFile().mkdirs();
                f.createNewFile();
                FileWriter writer = new FileWriter(path.toString());
                int nob = JToy.server.bcSize();
                long fts = -1;
                long lts = -1;
                int tCount = 0;
                for (int i = 0 ; i < nob ; i++) {
                    Types.Block b = JToy.server.nonBlockingdeliver(i);
                    for (Types.Transaction t : b.getDataList()) {
                        tCount++;
                        if (fts == -1) {
                            fts = b.getFooter().getTs();
                        }
                        lts = b.getFooter().getTs();
                        List<String> row = Arrays.asList(t.getTxID(),
                                String.valueOf(t.getClientID()), String.valueOf(b.getFooter().getTs()),
                                String.valueOf(t.getData().size()), String.valueOf(i),
                                String.valueOf(b.getHeader().getCreatorID()));
                        CSVUtils.writeLine(writer, row);
                    }
                }
                writer.flush();
                writer.close();
                writeSummery(pathString, tCount, fts, lts);
            } catch (IOException e) {
                logger.error("", e);
            }
        }

        void writeSummery(String pathString, int tCount, long fts, long lts) throws IOException {
            Path path = Paths.get(pathString,   String.valueOf(JToy.server.getID()), "summery.csv");
            File f = new File(path.toString());
            if (!f.exists()) {
                f.getParentFile().mkdirs();
                f.createNewFile();
            }
            FileWriter writer = new FileWriter(path.toString(), true);
            double time = ((double) lts - (double) fts) / 1000;
            int thrp = (int) (tCount / time);
            DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyyHH:mm:ss");
            List<String> row = Arrays.asList(dateFormat.format(new Date()), String.valueOf(JToy.server.getID()), String.valueOf(tCount),
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
                last = JToy.server.addTransaction(tx, cID);
            }
            serve();
            while ((!JToy.type.equals("m")) && (!last.equals("")) && JToy.server.isTxPresent(last) != 2) {
                Thread.sleep(5 * 1000);
            }
//            if (JToy.type.equals("m")) {
//                Thread.sleep(60 * 1000);
//            }
//            Thread.sleep(20 * 1000);
            stop();
            writeToScv(csvPath);
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
            ((byzantineBcServer) JToy.server).setByzSetting(fullByz, groups);
        }

}
