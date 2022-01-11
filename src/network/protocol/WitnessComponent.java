/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.protocol;

import java.nio.ByteBuffer;

/**
 *
 * @author virtu
 */
public class WitnessComponent {
    private final ByteBuffer component;
    private final VarInt varIntComponentLength;

    private WitnessComponent(VarInt varIntComponentLength, ByteBuffer component) {
        this.component = component;
        this.varIntComponentLength = varIntComponentLength;
    }
    
    public static final WitnessComponent fromSource(ByteBuffer component) {
        VarInt varIntComponentLength = VarInt.fromLong(component.capacity());
        return new WitnessComponent(varIntComponentLength, component);
    }
    
    public static final WitnessComponent fromBuffer(ByteBuffer buffer) {
        VarInt varIntComponentLength = VarInt.fromBuffer(buffer);
        int componentLength = (int)varIntComponentLength.getValue();
        ByteBuffer component = ByteBuffer.allocate(componentLength);
        buffer.get(component.array(), 0, componentLength);
        return new WitnessComponent(varIntComponentLength, component);
    }
    
    public final byte[] array() {
        ByteBuffer tmp = ByteBuffer.allocate(getTotalSize());
        tmp.put(varIntComponentLength.array());
        tmp.put(component.array());
        return tmp.array();
    }
        
    public int getComponentLength() {
        return component.capacity();
    }
    
    public int getTotalSize() {
        return varIntComponentLength.getSize() + (int)varIntComponentLength.getValue();
    }
    
}
