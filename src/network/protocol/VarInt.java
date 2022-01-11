/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.protocol;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import system.Utils;

/**
 *
 * @author Vratislav Bednařík
 */
public class VarInt {
    private long value;
    private int sizeIndex;
    private final static long[] MAX_VALUES = {0xfc, 0xffff, 0xffffffff, Long.MAX_VALUE};
    private final static long[] MASKS = {0xff, 0xffff, 0xffffffff, Long.MAX_VALUE};
    private final static int[] SIZES = {1, 3, 5, 9};

    private VarInt(long value) {
        this.value = value;
        sizeIndex = getSizeIndex();
    }

    private VarInt(long value, int sizeIndex) {
        this.value = value;
        this.sizeIndex = sizeIndex;
    }
    
    private int getSizeIndex() {
        int i = 0;
        while (value > MAX_VALUES[i])
            i++;
        return i;
    }
    
    public int getSize() {
        return SIZES[sizeIndex];
    }
    
    public final long getValue() {
        return value;
    }

    public final static VarInt fromLong(long number) {
        return new VarInt(number);
    }
    
    
    public final static VarInt fromBuffer(ByteBuffer buffer) {
        ByteBuffer tmp = ByteBuffer.allocate(8);
        tmp.order(ByteOrder.LITTLE_ENDIAN);
        int firstByte = buffer.get();
        int i = (firstByte + 4) & 255;
        int sizeIndex = (i>3) ? 0 : i;
        int size = SIZES[sizeIndex];
        byte[] tmpArray = tmp.array();
        tmpArray[0] = (byte)firstByte;
        buffer.get(tmpArray, 0, size-1);
        //debug// System.err.println("var_int size = " + size + " index = " + sizeIndex);
        //debug// System.err.println("var_int arra = " + Utils.getAsHex(tmp.array()));
        long value = tmp.getLong();
        return new VarInt(value, sizeIndex);
    }
    
    public final byte[] array() {
        int size = SIZES[sizeIndex];
        ByteBuffer tmp = ByteBuffer.allocate(9);
        byte[] rts = new byte[size];
        tmp.order(ByteOrder.LITTLE_ENDIAN);
        final int[] FIRST_BYTE = {(byte)value, 0xfd, 0xfe, 0xff};
        tmp.put((byte)FIRST_BYTE[sizeIndex]);
        tmp.putLong(value & MASKS[sizeIndex]);
        ((Buffer)tmp).rewind();
        //tmp.position(0);
        tmp.get(rts, 0, size);
        return rts;
    }

}
