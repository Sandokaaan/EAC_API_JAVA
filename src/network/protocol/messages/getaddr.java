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
public class getaddr extends Payload {

    public getaddr(ByteBuffer data) {
        super(data);
    }

    public static final getaddr fromSource() {
        ByteBuffer buffer = ByteBuffer.allocate(0);
        return new getaddr(buffer);
    }

    
    @Override
    public void dispatch(Peer peer) {
        // log
        // poslat ???
        System.out.println("--> todo getaddr dispatch");
    }
    
}
