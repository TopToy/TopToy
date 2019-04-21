package app;

import config.Config;
import servers.server;
import servers.Top;
//import TmoUtils.derbyUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JToy {
    private static org.apache.log4j.Logger logger;
//    static Config c = new Config();
    static server s; // = new ToyServer(Config.getAddress(), Config.getPort(), Config.getID());
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
            Path config = null;
            if (argv.length == 3) {
                config = Paths.get(argv[2]);
            }

            int serverID = Integer.parseInt(argv[0]);
            Config.setConfig(config, serverID);
            logger = org.apache.log4j.Logger.getLogger(JToy.class);
            type = argv[1];
            logger.debug("type is " + type);
            switch (type) {
                default:
                    s = new Top(serverID, Config.getN(), Config.getF(), Config.getC(), Config.getTMO(),
                            Config.getTMOInterval(), Config.getMaxTransactionsInBlock(), Config.getFastMode(),
                            Config.getObbcCluster(), Config.getWrbCluster(), Config.getCommCluster(),
                            Config.getABConfigHome(), type, Config.getServerCrtPath(), Config.getServerPrivKeyPath(),
                            Config.getCaRootPath());
                    break;
            }

            Cli parser = new Cli();
            Scanner scan = new Scanner(System.in).useDelimiter("\\n");
            while (true) {
                System.out.print("Toy>> ");
                if (!scan.hasNext()) {
                    break;
                }
                try {
                    parser.parse(getArgs(scan.next()));
                } catch (Exception e) {
                    logger.error(e);
                    System.exit(0);
                }
            }
//        } catch (Exception ex) {
//            logger.error("", ex);
//            System.exit(0);
//        }
    }
}
