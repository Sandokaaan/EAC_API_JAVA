/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.protocol;

import java.nio.ByteBuffer;
import system.Utils;

/**
 *
 * @author virtu
 */
public class VarStr {
    private final String string;

    private VarStr(String string) {
        this.string = string;
    }
    
    public final static VarStr fromString(String s) {
        return new VarStr(s);
    }
    
    public final String getString() {
        return string;
    }
    
    public final int getStringSize() {
        return string.length();
    }
    
    public final byte[] array() {
       int stringSize = getStringSize();
       VarInt varInt = VarInt.fromLong(stringSize);
       ByteBuffer tmp = ByteBuffer.allocate(varInt.getSize() + stringSize);
       tmp.put(varInt.array());
       tmp.put(string.getBytes(Utils.ASCII));
       return tmp.array();
    }
    
    public final static VarStr fromBuffer(ByteBuffer buffer) {
        VarInt varInt = VarInt.fromBuffer(buffer);
        int stringSize = (int)varInt.getValue();
        //debug// System.err.println(Utils.getAsHex(buffer.array()));
        //debug// System.err.println(" var_int stringSize = " + stringSize);
        ByteBuffer tmp = ByteBuffer.allocate(stringSize);
        buffer.get(tmp.array(), 0, stringSize);
        String string = new String(tmp.array(), Utils.ASCII);
        //debug// System.err.println(" var_str string = " + string);
        return new VarStr(string);
    }
    
}


