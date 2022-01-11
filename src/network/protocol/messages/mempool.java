/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.protocol.messages;

import java.nio.ByteBuffer;
import network.Peer;
import network.protocol.Payload;

/**
 *
 * @author virtu
 */
public class mempool extends Payload {

    public mempool(ByteBuffer data) {
        super(data);
    }

    public static final mempool fromSource() {
        ByteBuffer buffer = ByteBuffer.allocate(0);
        return new mempool(buffer);
    }
    
    @Override
    public void dispatch(Peer peer) {
        // log
        // poslat ???
        System.out.println("--> todo mempool dispatch");
    }
    
}
