/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.protocol.messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import network.Peer;
import network.protocol.InvType;
//import network.P2P.PoolMessage;
//import network.P2P.DaemonP2P;
import network.protocol.Inventory;
import network.protocol.InventoryVector;
import network.protocol.Payload;
import network.protocol.VarInt;

/**
 *
 * @author virtu
 */
public class inv extends Payload {

    public inv(ByteBuffer data) {
        super(data);
    }

    public static final inv fromSource(ArrayList<InventoryVector> inventories) {
        int invCount = inventories.size();
        VarInt count = VarInt.fromLong(invCount);
        int totalSize = count.getSize() + 36*invCount;
        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(count.array());
        inventories.forEach( (inventory) -> buffer.put(inventory.array()) );
        return new inv(buffer);
    }
    
    @Override
    public void dispatch(Peer peer) {
        // log
        // zaradit do databaze ?
        /*
        Inventory inventory = Inventory.fromBuffer(data);
        inventory.setPeer(peer);
        // test if we already have such data
        DaemonP2P daemon = peer.getPool().getDaemon();
        daemon.getInventoryManager().pushMessage(inventory);
        // databaze transakci
        // preposilani na dalsi uzly? - to by si mel udelat pool, nikoliv databaze
        */
        Inventory received = Inventory.fromBuffer(data);
        int count = received.getCount();
        InvType it = received.get(0).getType();
        System.out.println("--> received inv: " + count + " of " + it);
        if (it == InvType.MSG_BLOCK)
            peer.getParent().addMessage(it);
    }
    
}
