package das.data;

import java.util.ArrayList;

public class VoteData {
    ArrayList<ArrayList<Integer>> votes = new ArrayList<>();
    public VoteData() {
        votes.add(new ArrayList<>());
        votes.add(new ArrayList<>());
    }
    public void addVote(int pid, boolean vote) {
        if (votes.get(0).contains(pid) || votes.get(1).contains(pid)) return;
        int list = vote ? 1 : 0;
        votes.get(list).add(pid);
    }

    public int getVotersNum() {
        return votes.get(0).size() + votes.get(1).size();
    }

    public boolean getVoteReasult() {
        return votes.get(1).size() > votes.get(0).size();
    }
}
