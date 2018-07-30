package crypto;

import proto.Data;

public class rmfDigSig implements digitalSignature {
    @Override
    public String signMessage(Object toSign) {
        Data.Builder m = (Data.Builder) toSign;
        return pkiUtils.sign(String.valueOf(m.getMeta().getCid())
                + String.valueOf(m.getMeta().getSender())
                + String.valueOf(m.getMeta().getHeight())
                + new String (m.getData().toByteArray()));
    }

    @Override
    public boolean verifyMessage(int id, Object toVer) {
        Data m = (Data) toVer;
        return  pkiUtils.verify(id,
                String.valueOf(m.getMeta().getCid())
                        + String.valueOf(m.getMeta().getSender())
                        + String.valueOf(m.getMeta().getHeight())
                        + new String(m.getData().toByteArray()),
                m.getSig());
    }

    static public String sign(Object toSign) {
        return new rmfDigSig().signMessage(toSign);
    }

    static public boolean verify(int id, Object toVer) {
        return new rmfDigSig().verifyMessage(id, toVer);
    }
}
