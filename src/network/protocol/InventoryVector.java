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
public class InventoryVector {
    private final InvType type;
    private final Hash hash;

    private InventoryVector(InvType type, Hash hash) {
        this.type = type;
        this.hash = hash;
    }

    public Hash getHash() {
        return hash;
    }
    
    public static final InventoryVector fromSource(InvType type, Hash hash) {
        return new InventoryVector(type, hash);
    }
    
    public static final InventoryVector fromBuffer(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        InvType type = InvType.getName(buffer.getInt());
        Hash hash = Hash.fromBuffer(buffer);
        return new InventoryVector(type, hash);
    }

    public final InvType getType() {
        return type;
    }

    public final String getHashAsHex() {
        return hash.getAsHex();
    }
    
    public final byte[] array() {
        ByteBuffer tmp = ByteBuffer.allocate(36);
        tmp.order(ByteOrder.LITTLE_ENDIAN);
        tmp.putInt(type.getValue());
        hash.putItselfIntoByteBuffer(tmp);
        return tmp.array();
    }
    
}
