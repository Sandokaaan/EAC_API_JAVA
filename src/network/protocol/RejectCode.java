/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.protocol;

/**
 *
 * @author virtu
 */
    
public enum RejectCode { 
    REJECT_MALFORMED(0x01), REJECT_INVALID(0x10), REJECT_OBSOLETE(0x11), 
    REJECT_DUPLICATE(0x12), REJECT_NONSTANDARD(0x40), REJECT_DUST(0x41),
    REJECT_INSUFFICIENTFEE(0x42), REJECT_CHECKPOINT(0x43), UNDEFINED(99); 
    private final int code;
        
    private RejectCode(int code) {
        this.code = code;
    }
        
    public int getValue() {
        return code;
    }
    
    public static RejectCode getName(int code) {
        for (RejectCode value : RejectCode.values()) {
            if (code == value.code) {
                return value;
            }
        }
        return UNDEFINED;
    }
    
}