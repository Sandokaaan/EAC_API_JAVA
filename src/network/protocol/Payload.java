/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.protocol;

import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import network.Peer;
import system.Utils;

/**
 *
 * @author virtu
 */
public abstract class Payload {
    protected final ByteBuffer data;

    public Payload(ByteBuffer data) {
        this.data = data;
    }
    
    public final int getLenght() {
        return data.capacity();
    }
    
    public final int getChecksum() {
        return Utils.checksum(data.array());
    }
    
    public final void putDataIntoBuffer(ByteBuffer buffer) {
        buffer.put(data.array());
    }
    
    public abstract void dispatch(Peer peer);   
    
    @SuppressWarnings({"unchecked", "UseSpecificCatch"})
    public final static Payload ofName(String command, ByteBuffer data) {
        try {
            Class<?> specificPayload = Class.forName("network.protocol.messages." + command.trim());
            Constructor<?> specificConstructor = specificPayload.getConstructor(ByteBuffer.class);
            return (Payload) specificConstructor.newInstance(data);
        } catch (Exception ex) {
            //System.err.println(" ******* message \"" + command + "\" was not implemented yet");
            return null;
        }
    }

}
