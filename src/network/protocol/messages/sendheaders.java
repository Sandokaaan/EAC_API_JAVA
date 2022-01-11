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
public class sendheaders extends Payload{

    public sendheaders(ByteBuffer data) {
        super(data);
    }
    
    public static final sendheaders fromSource() {
        ByteBuffer buffer = ByteBuffer.allocate(0);
        return new sendheaders(buffer);
    }

    @Override
    public void dispatch(Peer peer) {
                // log
        // nastavit v peerovi, ze odpovedel
        System.out.println("--> received sendheaders but ignored");
        // Upon receipt of this message, the node is be permitted, but not required, to announce new blocks by headers command (instead of inv command). 
        // to by se dalo...
    }
}
