/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.protocol.messages;

//import database.PeerInfo;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
//import network.DaemonP2P;
import network.Peer;
import network.protocol.BlockHeader;
import network.protocol.Payload;
import network.protocol.VarInt;

/**
 *
 * @author virtu
 */
public class headers extends Payload {

    public headers(ByteBuffer data) {
        super(data);
    }
    
    public static final headers fromSource(Collection<BlockHeader> blockHeaders) {
        VarInt count = VarInt.fromLong(blockHeaders.size());
        int totalSize = count.getSize();
        totalSize = blockHeaders.stream().map((bh) -> bh.getTotalSize()).reduce(totalSize, Integer::sum);
        ByteBuffer rts = ByteBuffer.allocate(totalSize);
        rts.put(count.array());
        blockHeaders.forEach( (bh) -> rts.put(bh.array()) );
        return new headers(rts);        
    }

    @Override
    public void dispatch(Peer peer) {
        // log
        // ulozit headers do databaze
        /*
        VarInt count = VarInt.fromBuffer(data);
        int cnt = (int)count.getValue();
        ArrayList<BlockHeader> list = new ArrayList<>();
        DaemonP2P daemon = peer.getPool().getDaemon();
        System.out.println("--> todo headers dispatch: " + cnt + " from " + peer.getRemoteIP() + 
                " " + daemon.getHeight());
        for (int i=0; i< cnt; i++) {
            BlockHeader bh = BlockHeader.fromBuffer(data);
            list.add(bh);
        }
        peer.setHeaderResponseWait(false);
        daemon.getHeaderManager().pushMessages(list);*/
        System.out.println("--> todo headers dispatch");
    }
}
