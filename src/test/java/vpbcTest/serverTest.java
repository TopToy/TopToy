package vpbcTest;

import consensus.vpbc.vpbcServer;
import org.junit.jupiter.api.Test;
import utiles.SimpleValidator;

import static org.junit.jupiter.api.Assertions.assertEquals;
public class serverTest {
    @Test
    void singleServerTest() {
        vpbcServer s = new vpbcServer(0, new SimpleValidator(), "config/testsConfig");
//        assertEquals(1, 1);
    }

}
