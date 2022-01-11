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
public class OutPoint {
    private final Hash hash;
    private final int index;

    private OutPoint(Hash hash, int index) {
        this.hash = hash;
        this.index = index;
    }
    
    public static final OutPoint fromSource(Hash hash, int index) {
        return new OutPoint(hash, index);
    }
    
    public static final OutPoint fromBuffer(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        Hash hash = Hash.fromBuffer(buffer);
        int index = buffer.getInt();
        return new OutPoint(hash, index);
    }
    
    public final byte[] array() {
        ByteBuffer buffer = ByteBuffer.allocate(36);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        hash.putItselfIntoByteBuffer(buffer);
        buffer.putInt(index);
        return buffer.array();
    }
    
}
