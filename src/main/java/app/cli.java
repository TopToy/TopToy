package app;
import blockchain.cbcServer;
import config.Config;
import org.apache.commons.cli.*;

public class cli {

        private Options options = new Options();

        public cli() {
            options.addOption("help", "print this message");
            options.addOption("init", "init the server");
            options.addOption("serve", "start the server\n" +
                    "Note that before lunching the command, all other servers has to be initialized first");
            options.addOption("stop", "terminate the server");
            options.addOption("exit", "exit Toy terminal");
            options.addOption(Option.builder("tx")
                            .hasArg()
                            .desc("add new transaction")
                            .build()); // TODO: How?
        }

        void parse(String[] args) {
            CommandLineParser parser = new DefaultParser();
            try {
                CommandLine line = parser.parse(options, args);
                String[] in = line.getArgs();
                if (in[0].equals("help")) {
                    help();
                    return;
                }
                if (in[0].equals("init")) {
                    init();
                    System.out.println("Init server... [OK]");
                }

                if (in[0].equals("serve")) {
                    serve();
                    System.out.println("Serving... [OK]");
                }
                if (in[0].equals("stop")) {
                    stop();
                    System.out.println("Stopping server... [OK]");
                }
                if (in[0].equals("exit")) {
                    System.exit(0);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        private void help() {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Optimistic Total Ordering System ", options);
        }

        private void init() {

            JToy.server = new cbcServer(Config.getAddress(), Config.getPort(), Config.getID());
        }

        private void serve() {
            JToy.server.start();
            JToy.server.serve();
        }
        private void stop() {
            JToy.server.shutdown();
        }

}
