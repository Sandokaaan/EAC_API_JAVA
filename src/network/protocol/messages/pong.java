/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.protocol.messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import network.Peer;
import system.Utils;
import network.protocol.Payload;

/**
 *
 * @author virtu
 */
public class pong extends Payload {

    public pong(ByteBuffer data) {
        super(data);
    }

    public static final pong fromSource(long nonce) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(nonce);
        return new pong(buffer);
    }
    
    @Override
    public void dispatch(Peer peer) {
        data.order(ByteOrder.LITTLE_ENDIAN);
        long pingTime = data.getLong(0);
        long pongTime = Utils.timePrecise();
        int delay = (int)(pongTime - pingTime);
        peer.receivePong(delay);
        System.out.println("--> ping-pong delay " + delay + " ms from " + peer.getRemoteIP());
    }
    
}
