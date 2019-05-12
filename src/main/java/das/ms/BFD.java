package das.ms;
import proto.Types;

import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.subtractExact;

public class BFD {

    static private int n;
    static private int f;
    static private int workers;
    static private PriorityQueue<Types.suspected>[] sus;
    static private int threshold;
    static private boolean[] active;
    static private boolean[] byzActivity;

    public BFD(int n, int f, int workers, int threshold) {
        BFD.n = n;
        BFD.f = f;
        BFD.workers = workers;
        BFD.threshold = threshold;
        BFD.active = new boolean[workers];
        BFD.byzActivity = new boolean[workers];
        BFD.sus = new PriorityQueue[workers];
        for (int i = 0 ; i < workers ; i++) {
            BFD.active[i] = false;
            BFD.byzActivity[i] = false;
            BFD.sus[i] = new PriorityQueue(max(f, 1), Comparator.comparing(Types.suspected::getTmo).reversed());
        }

    }

    static public int getSuspected() {
        int ms = size(0);
        for (int i = 0 ; i < workers ; i++) {
            ms = min(ms, size(i));
        }
        return ms;
    }
    static public void activateAll() {
        for (int i = 0 ; i < workers ; i++) {
            activate(i);
        }
    }

    static public void deactivateAll() {
        for (int i = 0 ; i < workers ; i++) {
            deactivate(i);
        }
    }
    static public void activate(int w) {
        active[w] = true;
    }

    static public void deactivate(int w) {
        active[w] = false;
    }

    static public void reportByzActivity(int w) {
        byzActivity[w] = true;
    }

    static public boolean isSuspected(int id, int w) {
        return active[w] && !byzActivity[w] && sus[w].stream().anyMatch(s -> s.getId() == id);
    }

    static public boolean isExceedsThreshold(int tmo) {
        return tmo >= threshold;
    }

    static public void suspect(int id, int w, int tmo) {
        if (tmo  < threshold) return;
        if (sus[w].size() > f) {
            if (tmo <= sus[w].peek().getTmo()) return;
            sus[w].remove();
        }
        sus[w].add(Types.suspected.newBuilder().setId(id).setTmo(tmo).build());
    }

    static public void unsuspect(int id, int w) {
        sus[w].removeIf(s -> s.getId() == id);
    }

    static public int size(int w) {
        return sus[w].size();
    }
}
