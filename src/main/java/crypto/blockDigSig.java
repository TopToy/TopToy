package crypto;


import proto.Types.Block;

public class blockDigSig implements digitalSignature {
    private final static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(blockDigSig.class);
    protected class blockVerifyer {
        protected Block b;
        protected int cid;
        int cidSeries;
        protected String sig;
    }
    private blockVerifyer bv = new blockVerifyer();
    @Override
    public String signMessage(Object toSign) {
        logger.warn("block signature is not implemented as no block is directly signed");
        return null;
    }

    @Override
    public boolean verifyMessage(int id, Object toVer) {
        return  pkiUtils.verify(id,
                String.valueOf(bv.cidSeries) + String.valueOf(bv.cid)
                        + new String(bv.b.toByteArray()),
                bv.sig);
    }

    public static boolean verify(int id, int cidSeries, int cid, String sig, Block b) {
        blockDigSig bs = new blockDigSig();
        bs.bv.b = b;
        bs.bv.cid = cid;
        bs.bv.sig = sig;
        bs.bv.cidSeries = cidSeries;
        return bs.verifyMessage(id, null);
    }
}
