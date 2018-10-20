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
    public static void main(String argv[]) {
        int time = Integer.parseInt(argv[0]);
        int tSize = Integer.parseInt(argv[1]);
        int bareTxSize = Types.Transaction.newBuilder()
                .setClientID(0)
                .setTxID(UUID.randomUUID().toString())
                .build().getSerializedSize() + 8;
        int txSize = max(0, tSize - bareTxSize);
        int clientsNum = argv.length - 2;
        new Thread(() -> {
            executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(clientsNum);
            Random rand = new Random();
            for (int i = 0 ; i < clientsNum ; i++) {
//                System.out.println("Establishing client for " +  argv[i + 2]);
                int cID = rand.nextInt(100);
                txClient cl = new txClient(cID, argv[i + 2], 9876);
                clients.add(cl);
                executor.submit(() -> {
                    while (!stopped.get()) {
                        submitRandTxToServer(cl, txSize);
                    }
                });
            }
        }).start();

        try {
            System.out.println(format("sleeps for %d", time));
            Thread.sleep(time * 1000);
            stopped.set(true);
            Thread.sleep(2 * 1000);
            System.out.println(format("shutting down executor"));
            executor.shutdownNow();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (txClient c : clients) {
            c.shutdown();
        }
        System.exit(0);
    }

    static void submitRandTxToServer(txClient c, int txSize) {
        byte[] ts = Longs.toByteArray(System.currentTimeMillis());
        SecureRandom random = new SecureRandom();
        byte[] tx = new byte[txSize];
        random.nextBytes(tx);
        c.addTx(ArrayUtils.addAll(ts, tx));
    }
}
