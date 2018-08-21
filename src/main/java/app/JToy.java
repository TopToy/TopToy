package app;

import blockchain.cbcServer;
import config.Config;

import java.util.Scanner;

public class JToy {
    static cbcServer server;
    public static void main(String argv[]) {
        new Config();
        cli parser = new cli();
        Scanner scan = new Scanner(System.in);
        while (true) {
            System.out.print("Toy>>");
            parser.parse(scan.next().split(" "));
        }
    }
}
