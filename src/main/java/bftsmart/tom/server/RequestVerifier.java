/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bftsmart.tom.server;

import bftsmart.tom.core.messages.TOMMessage;

/**
 *
 * Classes that implement this interface are invoked within
 * das instances upon reception of a PROPOSE message
 * in order to enforce the "external validity". More precisely,
 * objects extending this class must verify if the requests
 * are valid in accordance to the application semantics (and not
 * an erroneous requests sent by a Byzantine leader).
 * 
 * @author joao
 */
public interface RequestVerifier {
    
    public boolean isValidRequest(TOMMessage request);
    
}
