/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.protocol;

import java.nio.ByteBuffer;
import java.util.Arrays;
import system.Utils;

/**
 *
 * @author Vratislav Bednařík
 */
public class Hash {
    public static final Hash NULLHASH = Hash.fromZero();
    public static final Hash GENESISHASH = Hash.fromHex("21717d4df403301c0538f1cb9af718e483ad06728bbcd8cc6c9511e2f9146ced");
    private final ByteBuffer data;

    private Hash(ByteBuffer data) {
        this.data = data;
    }
    
    public byte[] getCopy() {
        ByteBuffer rts = ByteBuffer.allocate(32);
        rts.put(data.array(), 0, 32);
        return rts.array();
    }
    
    // Copy only lowest 32 bytes
    public static final Hash fromHex(String s) {
       int strLen = s.length();
       if (strLen > 64)
           s = s.substring(strLen-64, 64);
       else if (strLen < 64) {
           char[] pad = new char[64-strLen];
           Arrays.fill(pad, '0');
           s = String.valueOf(pad) + s;
       }
       ByteBuffer buffer = ByteBuffer.allocate(32);
       for (int i=0; i<32; i++) {
           String r = s.substring((31-i)*2, (32-i)*2);
           int num = Integer.parseUnsignedInt(r, 16);
           buffer.put((byte)num);
       }
       return new Hash(buffer);
    }

    public static final Hash fromHash(Hash other) {
        ByteBuffer buffer = ByteBuffer.allocate(32);
        buffer.put(other.data.array(), 0, 32);
        return new Hash(buffer);
    }

    public static final Hash fromZero() {
        ByteBuffer buffer = ByteBuffer.allocate(32);
        return new Hash(buffer);
    }
    
    public static final Hash fromBuffer(ByteBuffer buffer) {
        ByteBuffer tmp = ByteBuffer.allocate(32);        
        buffer.get(tmp.array(), 0, 32);
        return new Hash(tmp);
    }

    // standard BigINteger is stored in Big-Endian, bitcoin protocol uses Little-Endian
    public final String getAsHex() {
        char[] tmp = new char[64];
        byte[] rawdata = this.data.array();
        int ofset = 0;
        for (int i = 31; i>=0; i--) {
            int k = Byte.toUnsignedInt(rawdata[i]);
            //System.err.println(k);
            tmp[ofset++] = Utils.BASE[k/16];
            tmp[ofset++] = Utils.BASE[k%16];
        }
        return String.valueOf(tmp);
    }
    
    public void putItselfIntoByteBuffer(ByteBuffer buffer) {
        buffer.put(data.array(), 0, 32);
    }

    @Override
    public String toString() {
        return getAsHex();
    }
    
    public static final Hash fromObject(Object o) {
        return Hash.fromBuffer(ByteBuffer.wrap(((byte[])o)));
    }

    public static final Hash fromBytes(byte[] b) {
        return Hash.fromBuffer(ByteBuffer.wrap((b)));
    }
    
    
    
}
