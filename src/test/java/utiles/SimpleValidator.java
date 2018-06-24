package utiles;

import consensus.vpbc.Validator;

public class SimpleValidator implements Validator {
    @Override
    public boolean validate(Object data) {
        return true;
    }
}
