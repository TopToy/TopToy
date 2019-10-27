package app;

import servers.BFTSMaRtOrderer;
import utils.Config;
import servers.Top;
import utils.GEH;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JToy {
    private static org.apache.log4j.Logger logger;
    static Top s;
//    static BFTSMaRtOrderer bftsmartServer;
    static String type;
    public static int serverID;
    public static boolean bftSMaRtSettings = false;
    public static void main(String[] argv) {
        mainImpl(argv);
//
    }

    private static String[] getArgs(String cmd) {
        List<String> matchList = new ArrayList<String>();
        Pattern regex = Pattern.compile("[^\\s\"']+|\"[^\"]*\"|'[^']*'");
        Matcher regexMatcher = regex.matcher(cmd);
        while (regexMatcher.find()) {
            matchList.add(regexMatcher.group());
        }
        return matchList.toArray(new String[0]);
    }

    private static void mainImpl(String argv[]) {
        Path config = null;
        if (argv.length == 3) {
            config = Paths.get(argv[2]);
        }

        serverID = Integer.parseInt(argv[0]);
        Config.setConfig(config, serverID);
        logger = org.apache.log4j.Logger.getLogger(JToy.class);
        Thread.setDefaultUncaughtExceptionHandler(new GEH());
        type = argv[1];
        logger.debug("type is " + type);


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
    }

    static void init() {
//        if (bftSMaRtSettings) {
//            bftsmartServer = new BFTSMaRtOrderer(serverID, 10,
//                    Config.getMaxTransactionsInBlock(), Config.getABConfigHome());
//            return;
//        }
        switch (type) {
            default:
                s = new Top(serverID, Config.getN(), Config.getF(), Config.getC(), Config.getTMO(),
                        Config.getMaxTransactionsInBlock(), Config.getObbcCluster(), Config.getWrbCluster(),
                        Config.getCommCluster(), Config.getABConfigHome(), type, Config.getServerCrtPath(),
                        Config.getServerPrivKeyPath(), Config.getCaRootPath());
                break;
        }
    }
}
