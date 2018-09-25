package app;

import config.Config;
import servers.bftsmartBCserver;
import servers.hlfBCserver;
import servers.server;
import servers.sg;
import utils.derbyUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JToy {
    private static org.apache.log4j.Logger logger;
//    static Config c = new Config();
    static server s; // = new cbcServer(Config.getAddress(), Config.getPort(), Config.getID());
    static String type;
    public static void main(String argv[]) {
        mainImpl(argv);
//
    }

    static String[] getArgs(String cmd) {
        List<String> matchList = new ArrayList<String>();
        Pattern regex = Pattern.compile("[^\\s\"']+|\"[^\"]*\"|'[^']*'");
        Matcher regexMatcher = regex.matcher(cmd);
        while (regexMatcher.find()) {
            matchList.add(regexMatcher.group());
        }
        return matchList.toArray(new String[0]);
    }

    static void mainImpl(String argv[]) {
        try {
            Config.setConfig(null, 0);
//        sg.createTxTable();
            Path config = null;
            if (argv.length == 3) {
                config = Paths.get(argv[2]);
            }

            int serverID = Integer.parseInt(argv[0]);
            Config.setConfig(config, serverID);
            logger = org.apache.log4j.Logger.getLogger(cli.class);
            type = argv[1];
            logger.debug("type is " + type);
            switch (type) {
                case "smr":
                    s = new bftsmartBCserver(serverID, Config.getF(),
                            Config.getMaxTransactionsInBlock(), Config.getSyncRBConfigHome());
                    break;
                case "hlf":
                    s = new hlfBCserver(serverID, Config.getF(),
                            Config.getMaxTransactionsInBlock(), Config.getSyncRBConfigHome());
                    break;
                default:
                    s = new sg(Config.getAddress(serverID), Config.getPort(serverID), serverID, Config.getF(), Config.getC(),
                            Config.getTMO(), Config.getTMOInterval(), Config.getMaxTransactionsInBlock(), Config.getFastMode(),
                            Config.getCluster(), Config.getRMFbbcConfigHome(), Config.getPanicRBConfigHome(),
                            Config.getSyncRBConfigHome(), type,
                            Config.getServerCrtPath(), Config.getServerTlsPrivKeyPath(), Config.getCaRootPath());
                    break;
            }

            cli parser = new cli();
            Scanner scan = new Scanner(System.in).useDelimiter("\\n");
            while (true) {
                System.out.print("Toy>> ");
                parser.parse(getArgs(scan.next()));
            }
        } catch (Exception ex) {
            logger.error("", ex);
            System.exit(0);
        }
    }
}
