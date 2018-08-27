package crypto;

import proto.Types.Data;


public class rmfDigSig implements digitalSignature {
    @Override
    public String signMessage(Object toSign) {
        Data.Builder m = (Data.Builder) toSign;
        return pkiUtils.sign(String.valueOf(m.getMeta().getCidSeries()) + String.valueOf(m.getMeta().getCid())
                + new String (DigestMethod.hash(m.getData().toByteArray())));
    }

    @Override
    public boolean verifyMessage(int id, Object toVer) {
        Data m = (Data) toVer;
        return  pkiUtils.verify(id,
                String.valueOf(m.getMeta().getCidSeries()) + String.valueOf(m.getMeta().getCid())
                        + new String(DigestMethod.hash(m.getData().toByteArray())),
                m.getSig());
    }

    static public String sign(Data.Builder toSign) {
        return new rmfDigSig().signMessage(toSign);
    }

    static public boolean verify(int id, Data toVer) {
        return new rmfDigSig().verifyMessage(id, toVer);
    }
}
