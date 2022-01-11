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
public class BlockHeader {
    private static final int BLOCK_VERSION = 2;
    private final int version;
    private final Hash prevBlock;
    private final Hash merkleRoot;
    private final int timestamp;
    private final int bits;
    private final int nonce;
    private final long txnCount;

    private BlockHeader(int version, Hash prevBlock, Hash merkleRoot, int timestamp, int bits, int nonce, long txnCount) {
        this.version = version;
        this.prevBlock = prevBlock;
        this.merkleRoot = merkleRoot;
        this.timestamp = timestamp;
        this.bits = bits;
        this.nonce = nonce;
        this.txnCount = txnCount;
    }

    public final Hash getBlockHash() {
        byte[] tohash = new byte[80];
        System.arraycopy(array(), 0, tohash, 0, 80);
        byte[] hash = system.Utils.double_sha256(tohash);
        return Hash.fromBytes(hash);
    }

    public Hash getPrevBlock() {
        return prevBlock;
    }
    
    public final static BlockHeader fromSource(int version, Hash prevBlock, Hash merkleRoot, int timestamp, int bits, int nonce, long txnCount) {
        return new BlockHeader(version, prevBlock, merkleRoot, timestamp, bits, nonce, txnCount);
    }
    
    public final static BlockHeader fromBuffer(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int version = buffer.getInt();
        Hash prevBlock = Hash.fromBuffer(buffer);
        Hash merkleRoot = Hash.fromBuffer(buffer);
        int timestamp = buffer.getInt();
        int bits = buffer.getInt();
        int nonce = buffer.getInt();
        VarInt varInt = VarInt.fromBuffer(buffer);
        long txnCount = varInt.getValue();
        return new BlockHeader(version, prevBlock, merkleRoot, timestamp, bits, nonce, txnCount);
    }
    
    public byte[] array() {
        VarInt varInt = VarInt.fromLong(txnCount);
        int totalSize = varInt.getSize() + 80;
        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(version);
        prevBlock.putItselfIntoByteBuffer(buffer);
        merkleRoot.putItselfIntoByteBuffer(buffer);
        buffer.putInt(timestamp);
        buffer.putInt(bits);
        buffer.putInt(nonce);
        buffer.put(varInt.array());
        return buffer.array();
    }
    
    public final int getTotalSize() {
        VarInt varInt = VarInt.fromLong(txnCount);
        return (varInt.getSize() + 80);
    }
}
