/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.protocol.messages;

import java.nio.ByteBuffer;
import network.Peer;
import system.Utils;
import network.protocol.Payload;

/**
 *
 * @author virtu
 */
public class verack extends Payload {

    public verack(ByteBuffer data) {
        super(data);
    }

    public static final verack fromSource() {
        ByteBuffer buffer = ByteBuffer.allocate(0);
        return new verack(buffer);
    }
    
    @Override
    public void dispatch(Peer peer) {
        // log
        int time = (int) Utils.timeNow();
        peer.receivedVerack(time);
        //System.out.println("--> todo verack dispatch");
        System.out.println("Peer " + peer.getRemoteIP() + ":" + peer.getRemotePort() + " connected.");
        
    }
    
}
