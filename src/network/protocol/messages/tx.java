/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.protocol.messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import network.Peer;
import network.protocol.Payload;
import network.protocol.TxIn;
import network.protocol.TxOut;
import network.protocol.TxType;
import network.protocol.TxWitness;
import network.protocol.VarInt;
import network.protocol.VarStr;
import network.protocol.WitnessComponent;

/**
 *
 * @author virtu
 */
public class tx extends Payload {

    private tx(ByteBuffer data) {
        super(data);
    }
    
    // + type 1 / 2 transaction, u 2 navic zprava!
    public static final tx fromSource(TxType version, boolean flag, Collection<TxIn> inTxs, Collection<TxOut> outTxs, Collection<WitnessComponent> components, int lockTime, String comment) {
        VarInt txInCount = VarInt.fromLong(inTxs.size());
        VarInt txOutCount = VarInt.fromLong(outTxs.size());
        // a comment property for version-2 transactions
        byte[] cmtArray = VarStr.fromString(comment).array();
        TxWitness txWitnesses = TxWitness.fromSource(components);
        int totalSize = (flag ? 2 : 0) + 8 
                        + txInCount.getSize() + (int)txInCount.getValue() 
                        + txOutCount.getSize() + (int)txOutCount.getValue() 
                        + (flag?txWitnesses.getTotalSize():0)
                        + ((version == TxType.WITH_COMMENTS) ? cmtArray.length : 0);
        ByteBuffer rts = ByteBuffer.allocate(totalSize);
        rts.order(ByteOrder.LITTLE_ENDIAN);
        rts.putInt(version.getValue());
        // SegWit ready flag
        if (flag) {
            rts.putShort((short)256);
        }
        rts.put(txInCount.array());
        inTxs.forEach((inTx) -> rts.put(inTx.array()) );
        rts.put(txOutCount.array());
        outTxs.forEach((outTx) -> rts.put(outTx.array()) );
        // only in the case of a SegWit block (not supported in the legacy network
        if (flag)
            rts.put(txWitnesses.array());
        rts.putInt(lockTime);
        if (version == TxType.WITH_COMMENTS)
            rts.put(cmtArray);
        return new tx(rts);
    }

    public final void putInselfIntoBuffer(ByteBuffer buffer) {
        buffer.put(data.array());
    }
    
    @Override
    public void dispatch(Peer peer) {
        // log
        // ulozit tx do databaze
        System.out.println("--> todo tx dispatch");
    }
    // dodelat
}
