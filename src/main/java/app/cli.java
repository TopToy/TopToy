package app;
import blockchain.cbcServer;
import config.Config;
import org.apache.commons.cli.*;

import java.util.Arrays;

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

}
