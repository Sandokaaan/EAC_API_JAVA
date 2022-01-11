/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

/**
 *
 * @author virtu
 */
public class BlockRange {
    private final ArrayList<Hash> starts;
    private final Hash stop;

    private BlockRange(ArrayList<Hash> starts, Hash stop) {
        this.starts = starts;
        this.stop = stop;
    }
    
    public final static BlockRange fromSource(ArrayList<Hash> starts, Hash stop) {
        return new BlockRange(starts, stop);
    }
    
    public static final BlockRange fromBuffer(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int version = buffer.getInt();
        VarInt hashCount = VarInt.fromBuffer(buffer);
        int count = (int)hashCount.getValue();
        ArrayList<Hash> starts = new ArrayList<>();
        for (int i=0; i<count; i++) {
            starts.add(Hash.fromBuffer(buffer));
        }
        Hash stop = Hash.fromBuffer(buffer);
        return new BlockRange(starts, stop);
    }
    
    public final int getCount() {
        return starts.size();
    }
    
    public final byte[] array() {
        int size = (1 + starts.size()) * 32;
        ByteBuffer tmp = ByteBuffer.allocate(size);
        starts.forEach( (hash) -> hash.putItselfIntoByteBuffer(tmp) );
        stop.putItselfIntoByteBuffer(tmp);
        return tmp.array();
    }
}

