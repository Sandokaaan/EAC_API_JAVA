/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 *
 * @author virtu
 */
public class TxOut {
    private final long value;
    private final int pkScriptLength;
    private final ByteBuffer pkScript;

    private TxOut(long value, int pkScriptLength, ByteBuffer pkScript) {
        this.value = value;
        this.pkScriptLength = pkScriptLength;
        this.pkScript = pkScript;
    }
    
    public static final TxOut fromSource(long value, int pkScriptLength, ByteBuffer pkScript) {
        return new TxOut(value, pkScriptLength, pkScript);
    }
    
    public static final TxOut fromBuffer(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        long value = buffer.getLong();
        VarInt varInt = VarInt.fromBuffer(buffer);
        int pkScriptLength = (int)varInt.getValue();
        ByteBuffer pkScript = ByteBuffer.allocate(pkScriptLength);
        buffer.get(pkScript.array(), 0, pkScriptLength);
        return new TxOut(value, pkScriptLength, pkScript);
    }
    
    public final byte[] array() {
        VarInt varInt = VarInt.fromLong(value);
        int totalSize = 8 + varInt.getSize() + pkScript.array().length;
        ByteBuffer tmp = ByteBuffer.allocate(totalSize);
        tmp.order(ByteOrder.LITTLE_ENDIAN);
        tmp.putLong(value);
        tmp.put(varInt.array());
        tmp.put(pkScript);
        return tmp.array();
    }
}
