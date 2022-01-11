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
public enum InvType { 
    ERROR(0), MSG_TX(1), MSG_BLOCK(2), MSG_FILTERED_BLOCK(3), MSG_CMPCT_BLOCK(4), UNDEFINED(99); 
    private final int code;

    private InvType(int code) {
        this.code = code;
    }

    public int getValue() {
        return code;
    }

    public static InvType getName(int code) {
        for (InvType value : InvType.values()) {
            if (code == value.code) {
                return value;
            }
        }
        return UNDEFINED;
    }
    
}