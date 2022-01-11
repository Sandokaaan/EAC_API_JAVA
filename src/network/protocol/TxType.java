/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.protocol;

/**
 *
 * @author Vratislav Bednařík
 */
public enum TxType {
    STANDARD(1), WITH_COMMENTS(2), UNDEFINED(0); 
    private final int code;

    private TxType(int code) {
        this.code = code;
    }

    public int getValue() {
        return code;
    }

    public static TxType getName(int code) {
        for (TxType value : TxType.values()) {
            if (code == value.code) {
                return value;
            }
        }
        return UNDEFINED;
    }
    
}