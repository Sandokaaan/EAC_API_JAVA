/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.protocol.messages;

//import database.PeerInfo;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import network.Peer;
import network.protocol.NetAddr;
import network.protocol.Payload;
import network.protocol.VarInt;

/**
 *
 * @author virtu
 */
public class addr extends Payload {

    public addr(ByteBuffer data) {
        super(data);
    }

    public static final addr fromSource(ArrayList<NetAddr> addrs) {
        int addrCount = addrs.size();
        VarInt count = VarInt.fromLong(addrCount);
        int totalSize = count.getSize() + 30*addrCount;
        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(count.array());
        addrs.forEach( (addr) -> buffer.put(addr.array(true)) );
        return new addr(buffer);
    }
    
    @Override
    public void dispatch(Peer peer) {
        // log
        // zaradit do databaze ?
/*        VarInt count = VarInt.fromBuffer(data);
        int cnt = (int)count.getValue();
        System.out.println("--> todo addr dispatch: " + cnt + " from " + peer.getRemoteIP());
        ArrayList<PeerInfo> list = new ArrayList<>();
        for (int i=0; i< cnt; i++) {
            NetAddr netAddr = NetAddr.fromBuffer(data, true);
            list.add(new PeerInfo(netAddr.getIp(), (int)netAddr.getPort(), netAddr.getTime(), peer));
        }
        peer.getPool().getDaemon().getNodeDB().pushMessages(list);*/
        System.out.println("--> todo addr dispatch");
    }
    
}
