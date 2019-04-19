package das.ms;

import com.google.common.collect.MinMaxPriorityQueue;
import proto.Types;

import java.util.Comparator;

public class BFD {

    static private int n;
    static private int f;
    static private MinMaxPriorityQueue<Types.suspected> sus;
    static private int origTmo;
    static private int thresholdTries;

    public BFD(int n, int f, int origTmo, int thresholdTries) {
        BFD.n = n;
        BFD.f = f;
        BFD.origTmo = origTmo;
        BFD.thresholdTries = thresholdTries;
        BFD.sus = MinMaxPriorityQueue
                .orderedBy(Comparator.comparing(Types.suspected::getTmo).reversed())
                .maximumSize(f)
                .create();
    }

    static public void handleSuspection(int id, int tmo) {
        if (tmo < thresholdTries * origTmo) {
            sus.removeIf(s -> s.getId() == id);
            return;
        }
        BFD.sus.add(Types.suspected.newBuilder().setId(id).setTmo(tmo).build());
    }

    static public boolean isSuspected(int id) {
        return sus.stream().anyMatch(s -> s.getId() == id);
    }
}
