package app;

import blockchain.asyncBcServer;
import blockchain.bcServer;
import blockchain.byzantineBcServer;
import blockchain.cbcServer;
import config.Config;
import org.checkerframework.checker.units.qual.C;
import serverGroup.sg;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JToy {
    private static org.apache.log4j.Logger logger;
//    static Config c = new Config();
    static sg server; // = new cbcServer(Config.getAddress(), Config.getPort(), Config.getID());
    static String type;
    public static void main(String argv[]) {
        Path config = null;
        if (argv.length == 3) {
            config = Paths.get(argv[2]);
        }

        int serverID = Integer.parseInt(argv[0]);
        Config.setConfig(config, serverID);
        logger = org.apache.log4j.Logger.getLogger(cli.class);
        type = argv[1];
        logger.debug("type is " + type);
        server = new sg(Config.getAddress(serverID), Config.getPort(serverID), serverID, Config.getF(), Config.getC(),
                Config.getTMO(), Config.getTMOInterval(), Config.getMaxTransactionsInBlock(), Config.getFastMode(),
                Config.getCluster(), Config.getRMFbbcConfigHome(), Config.getPanicRBConfigHome(),
                Config.getSyncRBConfigHome(), type,
                Config.getServerCrtPath(), Config.getServerTlsPrivKeyPath(), Config.getCaRootPath());
        // possible types: r - regular, m - mute, a - async, bs - selective byz, bf - full byz
//        switch (type) {
//            case "a":
//                server = new asyncBcServer(Config.getAddress(serverID), Config.getPort(serverID), serverID);
//                break;
//            case "bs":
//                server = new byzantineBcServer(Config.getAddress(serverID), Config.getPort(serverID), serverID);
//                break;
//            case "bf":
//                server = new byzantineBcServer(Config.getAddress(serverID), Config.getPort(serverID), serverID);
//                break;
//            default:
//                server = new cbcServer(Config.getAddress(serverID), Config.getPort(serverID), serverID);
//                break;
//        }

        cli parser = new cli();
        Scanner scan = new Scanner(System.in).useDelimiter("\\n");
        while (true) {
            System.out.print("Toy>> ");
            parser.parse(getArgs(scan.next()));
        }
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
}
