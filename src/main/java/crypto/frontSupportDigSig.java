//package crypto;
//
//import proto.frontSupport;
//
//public class frontSupportDigSig implements digitalSignature {
//    @Override
//    public String signMessage(Object toSign) {
//        frontSupport.Builder s = (frontSupport.Builder) toSign;
//        return pkiUtils.sign(String.valueOf(s.getSid()) +
//                String.valueOf(s.getId()) + String.valueOf(s.getHeight()));
//    }
//
//    @Override
//    public boolean verifyMessage(int id, Object toVer) {
//        frontSupport s = (frontSupport) toVer;
//        return pkiUtils.verify(id, String.valueOf(s.getSid()) + String.valueOf((s.getId()) + String.valueOf(s.getHeight())), s.getSig());
//    }
//
//    static public String sign(frontSupport.Builder s) {
//       return new frontSupportDigSig().signMessage(s);
//    }
//
//    static public boolean verify(int id, frontSupport s) {
//        return new frontSupportDigSig().verifyMessage(id, s);
//    }
//}
