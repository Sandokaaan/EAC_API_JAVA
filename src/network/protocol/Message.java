/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import network.Peer;
import system.Utils;

/**
 *
 * @author virtu
 */
public class Message {
    private final MessageHeader msgHeader;
    private final Payload payload;
    private Peer peer;
    private final int timestamp;

    private Message(MessageHeader msgHeader, Payload payload, Peer peer) {
        this.payload = payload;
        this.msgHeader = msgHeader;
        this.peer = peer;
        timestamp = (int) Utils.timeNow();
    }

    public void setPeer(Peer peer) {
        this.peer = peer;
    }
    
    public final boolean isConnectMsg() {
        return ( (msgHeader == null) && (payload == null) && (peer != null) );
    }
    
    public static Message fromConnect(Peer peer) {
        return new Message(null, null, peer);
    }

    public Peer getPeer() {
        return peer;
    }
    
    public static Message fromSource(String command, Payload payload, Peer peer) {
        if (payload == null)
            return null;
        MessageHeader msgHeader = MessageHeader.fromSource(command, payload.getLenght(), payload.getChecksum());
        if (msgHeader == null)
            return null;
        return new Message(msgHeader, payload, peer);
    }
    
    public final byte[] array() {
        ByteBuffer rts = ByteBuffer.allocate(24 + msgHeader.getPayloadLength());
        rts.order(ByteOrder.LITTLE_ENDIAN);
        rts.put(msgHeader.array());
        payload.putDataIntoBuffer(rts);
        return rts.array();
    }
    
    public final void dispatch() {
        if (payload != null)
            if (peer != null)
                payload.dispatch(peer);
    }
    
    public final String getMessageType() {
        return msgHeader.getCommand();
    }
}
