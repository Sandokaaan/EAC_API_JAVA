/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.protocol.messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.Buffer;
import java.util.Collection;
//import network.DaemonP2P;
import network.Peer;
import network.protocol.BlockHeader;
import network.protocol.Payload;
import network.protocol.VarInt;

/**
 *
 * @author virtu
 */
public class block extends Payload {

    public block(ByteBuffer data) {
        super(data);
    }
    
    public static final block fromSource(BlockHeader blockHeader, Collection<tx> txs) {
        byte[] bhArray = blockHeader.array();
        VarInt txnCount = VarInt.fromLong(txs.size());
        int totalSize = bhArray.length + txnCount.getSize();
        totalSize = txs.stream().map((t) -> t.getLenght()).reduce(totalSize, Integer::sum);
        ByteBuffer rts = ByteBuffer.allocate(totalSize);
        rts.put(bhArray);
        rts.put(txnCount.array());
        txs.forEach( (t) -> t.putInselfIntoBuffer(rts) );
        return new block(rts);
    }
    
    public final BlockHeader getHeader() {
        ((Buffer)data).position(0);
        data.order(ByteOrder.LITTLE_ENDIAN);
        BlockHeader header = BlockHeader.fromBuffer(data);
        return header;
    }
    
    public final byte[] array() {
        return data.array();
    }

    @Override
    public void dispatch(Peer peer) {
        // log
        // ulozit tx do databaze
        System.out.println("--> block dispatch");    
        /*
        DaemonP2P daemon = peer.getPool().getDaemon();
        daemon.getBlockManager().pushMessage(this);
        peer.incAnswers();*/
    }

}
