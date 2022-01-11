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
public class TxIn {
    private final OutPoint previousOutput;
    private final int scriptLength;
    private final ByteBuffer signatureScript;
    private final int sequence;

    public TxIn(OutPoint previousOutput, int scriptLength, ByteBuffer signatureScript, int sequence) {
        this.previousOutput = previousOutput;
        this.scriptLength = scriptLength;
        this.signatureScript = signatureScript;
        this.sequence = sequence;
    }
    
    public static final TxIn fromSource(OutPoint previousOutput, int scriptLength, ByteBuffer signatureScript, int sequence) {
        return new TxIn(previousOutput, scriptLength, signatureScript, sequence);
    }
            
    public static final TxIn fromBuffer(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        OutPoint previousOutput = OutPoint.fromBuffer(buffer);
        VarInt varInt = VarInt.fromBuffer(buffer);
        int scriptLength = (int)varInt.getValue();
        ByteBuffer signatureScript = ByteBuffer.allocate(scriptLength);
        buffer.get(signatureScript.array(), 0, scriptLength);
        int sequence = buffer.getInt();
        return new TxIn(previousOutput, scriptLength, signatureScript, sequence);
    }
    
    public final byte[] array() {
        VarInt varInt = VarInt.fromLong(scriptLength);
        int totalSize = 40 + varInt.getSize() + signatureScript.array().length;
        ByteBuffer tmp = ByteBuffer.allocate(totalSize);
        tmp.order(ByteOrder.LITTLE_ENDIAN);
        tmp.put(previousOutput.array());
        tmp.put(varInt.array());
        tmp.put(signatureScript.array());
        tmp.putInt(sequence);
        return tmp.array();
    }
    
}
