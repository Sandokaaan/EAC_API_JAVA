/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.protocol.messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import network.Peer;
import network.protocol.Inventory;
import network.protocol.InventoryVector;
import network.protocol.Payload;

/**
 *
 * @author virtu
 */
public class getdata extends Payload {

    public getdata(ByteBuffer data) {
        super(data);
    }

    public static final getdata fromSource(InventoryVector ... invs) {
        Inventory inv = Inventory.fromSource(invs);
        byte[] tmp = inv.array();
        ByteBuffer buffer = ByteBuffer.allocate(tmp.length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(tmp);
        return new getdata(buffer);
    }

    @Override
    public void dispatch(Peer peer) {
        // log
        // odeslat pozadovana data?
        System.out.println("--> todo getdata dispatch");
    }
}
