package crypto;

public interface digitalSignature {

    String signMessage(Object toSign);

    boolean verifyMessage(int id, Object toVer);
}
