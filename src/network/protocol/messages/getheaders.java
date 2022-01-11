/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.protocol.messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import network.Peer;
import network.protocol.BlockRange;
import network.protocol.Payload;
import network.protocol.VarInt;

/**
 *
 * @author virtu
 */
public class getheaders extends Payload {

    public getheaders(ByteBuffer data) {
        super(data);
    }

    public static final getheaders fromSource(ArrayList<network.protocol.Hash> starts, network.protocol.Hash stop) {
        BlockRange range = BlockRange.fromSource(starts, stop);
        int blockCount = range.getCount();
        VarInt count = VarInt.fromLong(blockCount);
        int totalSize = 4 + count.getSize() + blockCount*32 + 32;
        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(version.OUR_VERSION);
        buffer.put(count.array());
        buffer.put(range.array());
        return new getheaders(buffer);
    }

    @Override
    public void dispatch(Peer peer) {
        // log
        // odeslat pozadovane bloky 
        System.out.println("--> received getheaders but ignored");
    }
    
}