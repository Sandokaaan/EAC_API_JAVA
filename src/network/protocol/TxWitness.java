/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.protocol;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author virtu
 */
public class TxWitness {
    private final ArrayList<WitnessComponent> witnessComponents;
    private final VarInt witnessComponentsCount;

    private TxWitness(Collection<WitnessComponent> components) {
        witnessComponents = new ArrayList<>();
        components.forEach( (component) -> witnessComponents.add(component) );
        witnessComponentsCount = VarInt.fromLong(components.size());
    }

    public static final TxWitness fromSource(Collection<WitnessComponent> components) {
        return new TxWitness(components);
    }
    
    public static final TxWitness fromBuffer(ByteBuffer buffer) {
        VarInt varInt = VarInt.fromBuffer(buffer);
        int n = (int)varInt.getValue();
        ArrayList<WitnessComponent> components = new ArrayList<>();
        for (int i=0; i<n; i++) 
            components.add(WitnessComponent.fromBuffer(buffer));
        return new TxWitness(components);
    }
    
    public final byte[] array() {
        ByteBuffer tmp = ByteBuffer.allocate(getTotalSize());
        tmp.put(witnessComponentsCount.array());
        witnessComponents.forEach( (component) ->  tmp.put(component.array()) );
        return tmp.array();
    }
    
    public final int getTotalSize() {
        int totalSize = witnessComponentsCount.getSize();
        totalSize = witnessComponents.stream().map( (component) -> component.getTotalSize() ).reduce(totalSize, Integer::sum);
        return totalSize;
    }
}
