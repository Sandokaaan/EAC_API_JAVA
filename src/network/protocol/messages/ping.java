/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.protocol.messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import network.Peer;
import network.protocol.Message;
import system.Utils;
import network.protocol.Payload;

/**
 *
 * @author virtu
 */
public class ping extends Payload {

    public ping(ByteBuffer data) {
        super(data);
    }

    public static final ping fromSource() {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(Utils.timePrecise());
        return new ping(buffer);
    }

    
    @Override
    public void dispatch(Peer peer) {
        // log
        // poslat pong
        data.order(ByteOrder.LITTLE_ENDIAN);
        long pingTime = data.getLong(0);
        peer.sendMessage(Message.fromSource("pong", pong.fromSource(pingTime), peer));
        System.out.println("--> ping responded with " + pingTime + " to " + peer.getRemoteIP());
    }
    
}
