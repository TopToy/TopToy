package consensus.vpbc;

import java.io.Serializable;

public class vpbcMessage implements Serializable {
    int client;
    int consID;
    byte[] data;

    public vpbcMessage(int client, int consID, byte[] data) {
        this.client = client;
        this.consID = consID;
        this.data = data;
    }
}
