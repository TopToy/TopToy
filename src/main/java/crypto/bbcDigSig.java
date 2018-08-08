package crypto;

import proto.BbcProtos;

public class bbcDigSig implements digitalSignature {
    @Override
    public String signMessage(Object toSign) {
        BbcProtos.BbcMsg.Builder m = (BbcProtos.BbcMsg.Builder) toSign;
        return pkiUtils.sign(String.valueOf(m.getCidSeries()) + String.valueOf(m.getCid()) +
                String.valueOf(m.getPropserID()) + String.valueOf(m.getVote()));
    }

    @Override
    public boolean verifyMessage(int id, Object toVer) {
        BbcProtos.BbcMsg m = (BbcProtos.BbcMsg) toVer;
        return pkiUtils.verify(id, String.valueOf(m.getCidSeries()) + String.valueOf(m.getCid()) +
                String.valueOf(m.getPropserID()) + String.valueOf(m.getVote()), m.getSig());
    }

    public static String sign(BbcProtos.BbcMsg.Builder toSign) {
        return new bbcDigSig().signMessage(toSign);
    }

    public static boolean verify(int id, BbcProtos.BbcMsg toVer) {
        return new bbcDigSig().verifyMessage(id, toVer);
    }
}
