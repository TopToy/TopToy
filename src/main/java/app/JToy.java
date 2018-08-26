package app;

import blockchain.cbcServer;
import config.Config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JToy {
//    static Config c = new Config();
    static cbcServer server; // = new cbcServer(Config.getAddress(), Config.getPort(), Config.getID());
    public static void main(String argv[]) {
//        new Config();
        Path config = null;
        if (argv.length == 2) {
            config = Paths.get(argv[1]);
        }

        int serverID = Integer.parseInt(argv[0]);
        Config.setConfig(config, serverID);
        server =  new cbcServer(Config.getAddress(serverID), Config.getPort(serverID), serverID);
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
