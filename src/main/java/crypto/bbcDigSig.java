package crypto;

import proto.BbcProtos;

public class bbcDigSig implements digitalSignature {
    @Override
    public String signMessage(Object toSign) {
        BbcProtos.BbcMsg.Builder m = (BbcProtos.BbcMsg.Builder) toSign;
        return pkiUtils.sign(String.valueOf(m.getId()) +
                String.valueOf(m.getClientID()) + String.valueOf(m.getVote()));
    }

    @Override
    public boolean verifyMessage(int id, Object toVer) {
        BbcProtos.BbcMsg m = (BbcProtos.BbcMsg) toVer;
        return pkiUtils.verify(id, String.valueOf(m.getId()) +
                String.valueOf(m.getClientID()) + String.valueOf(m.getVote()), m.getSig());
    }

    public static String sign(Object toSign) {
        return new bbcDigSig().signMessage(toSign);
    }

    public static boolean verify(int id, Object toVer) {
        return new bbcDigSig().verifyMessage(id, toVer);
    }
}
