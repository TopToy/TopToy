package app;

import clients.txClient;
import com.google.common.primitives.Longs;
import org.apache.commons.lang.ArrayUtils;
import proto.Types;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.StrictMath.max;
import static java.lang.String.format;

public class clientsAgent {
    private static List<txClient> clients = new ArrayList<>();
    private static ThreadPoolExecutor executor; // = (ThreadPoolExecutor) Executors.newFixedThreadPool(clients);
    private static AtomicBoolean stopped = new AtomicBoolean(false);
    static int cln = 1;
    static double total = 0;
    public static void main(String argv[]) {
        int time = Integer.parseInt(argv[0]);
        int tSize = Integer.parseInt(argv[1]);
        int bareTxSize = Types.Transaction.newBuilder()
                .setClientID(0)
                .setTxID(UUID.randomUUID().toString())
                .build().getSerializedSize() + 8;
        int txSize = max(0, tSize - bareTxSize);
        System.out.println("Tx size is: " + String.valueOf(txSize + bareTxSize));
        int clientsNum = argv.length - 2;
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(clientsNum * cln);
        Random rand = new Random();
//        for (int j = 0 ; j < 5 ; j++) {
        for (int i = 0 ; i < clientsNum ; i++) {
            for (int j = 0 ; j < cln ; j++) {
//                System.out.println("Establishing client for " +  argv[i + 2]);
                int cID = rand.nextInt(10000);
                txClient cl = new txClient(cID, argv[i + 2], 9876);
                clients.add(cl);
                executor.submit(() -> {
                    while (!stopped.get()) {
                        if (submitRandTxToServer(cl, txSize) == -1) {
                            cl.shutdown();
                            return;
                        }
                    }
                });
            }
        }
        System.out.println(format("Total of %d clients", clients.size()));
        try {
            System.out.println(format("sleeps for %d", time));
            Thread.sleep(time * 1000);
            stopped.set(true);
            Thread.sleep(2 * 1000);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int total2 = 0;
        for (txClient c : clients) {
            total += c.shutdown();
            total2 += c.getTxCount();
            System.out.println(format("Client %d send %d tx in total with throughput of %s", c.getID(),
                    c.getTxCount(), String.valueOf(c.getAvgTx())));
        }
        System.out.println(format("shutting down executor"));
        executor.shutdown();
        System.out.println(format("total %d ; tx/sec %s", total2, String.valueOf(total)));
        System.exit(0);
    }

    static int submitRandTxToServer(txClient c, int txSize) {
        byte[] ts = Longs.toByteArray(System.currentTimeMillis());
        SecureRandom random = new SecureRandom();
        byte[] tx = new byte[txSize];
        random.nextBytes(tx);
        return c.addTx(ArrayUtils.addAll(ts, tx));
    }
}
