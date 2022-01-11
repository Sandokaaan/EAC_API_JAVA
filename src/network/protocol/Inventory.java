/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author virtu
 */
public class Inventory {
    private final ArrayList<InventoryVector> vectors;

    private Inventory(ArrayList<InventoryVector> vectors) {
        this.vectors = vectors;
    }

    private Inventory() {
        vectors = new ArrayList<>();
    }
    
    public static final Inventory empty() {
        return new Inventory();
    }
    
    public static final Inventory fromSource(InventoryVector ... invs) {
        ArrayList<InventoryVector> tmp = new ArrayList<>();
        tmp.addAll(Arrays.asList(invs));
        return new Inventory(tmp);
    }
    
    public static final Inventory fromBuffer(ByteBuffer buffer) {
        VarInt varInt = VarInt.fromBuffer(buffer);
        int count  = (int)varInt.getValue();
        ArrayList<InventoryVector> tmp = new ArrayList<>();
        for (int i=0; i<count; i++) {
            InventoryVector inv = InventoryVector.fromBuffer(buffer);
            tmp.add(inv);
        }
        return new Inventory(tmp);        
    }
    
    public int getCount() {
        return vectors.size();
    }

    public ArrayList<InventoryVector> getVectors() {
        return vectors;
    }
    
    public InventoryVector get(int i) {
        return vectors.get(i);  // check out of range
    }
    
    public void put(InventoryVector x) {
        vectors.add(x);
    }
    
    public final byte[] array() {
        int count = getCount();
        VarInt varInt = VarInt.fromLong(count);
        int totalSize = varInt.getSize() + count*36;
        ByteBuffer tmp = ByteBuffer.allocate(totalSize);
        tmp.order(ByteOrder.LITTLE_ENDIAN);
        tmp.put(varInt.array());
        vectors.forEach( (inv) -> tmp.put(inv.array()) );
        return tmp.array();
    }

    
}
