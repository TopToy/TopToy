import examples.helloworldtls.HelloWorldClientTls;
import examples.helloworldtls.HelloWorldServerTls;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

public class JToy {

    public static void main(String argv[]) {
        new Thread(() -> {
            try {
                HelloWorldServerTls.run();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        try {
            Thread.sleep(10 * 1000);
            HelloWorldClientTls.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
