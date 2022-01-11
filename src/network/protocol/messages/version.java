/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.protocol.messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.Buffer;
import network.Peer;
import system.Utils;
import network.protocol.Message;
import network.protocol.NetAddr;
import network.protocol.Payload;
import network.protocol.VarStr;
import system.Config;

/**
 *
 * @author virtu
 */
public class version extends Payload{
    public static final int OUR_VERSION = Config.getConfig().protocolVersion;
    public static final int MINIMAL_VERSION = OUR_VERSION;
    public static final String USER_AGENT  = Config.getConfig().p2pPeerName;
    public static final long SERVICES = 1024;  // client only, none services

    public version(ByteBuffer data) {
        super(data);
    }

    public static final version fromSource(String ipL, int portL, String ipR, int portR, int startHeight, boolean relay) {
        byte[] userAgent = VarStr.fromString(USER_AGENT).array();
        int size = 4 + 8 + 8 + 26 + 26 + 8 + 4 + 1 + userAgent.length;
        ByteBuffer tmp = ByteBuffer.allocate(size);
        tmp.order(ByteOrder.LITTLE_ENDIAN);
        tmp.putInt(OUR_VERSION);
        tmp.putLong(SERVICES);
        tmp.putLong(Utils.timeNow());
        tmp.put(NetAddr.fromSource(ipR, portR, SERVICES, false).array(false));
        tmp.put(NetAddr.fromSource(ipL, portL, SERVICES, false).array(false));
        tmp.putLong(Utils.RANDOM.nextLong());
        tmp.put(userAgent);
        tmp.putInt(startHeight);
        tmp.put(relay ? (byte)1 : (byte)0);
        //System.err.println(" --- Version fromSource --- " + Utils.getAsHex(tmp.array()));
        return new version(tmp);        
    }
    
    @Override
    public void dispatch(Peer peer) {
        ((Buffer)data).position(0);
        data.order(ByteOrder.LITTLE_ENDIAN);
        int version = data.getInt();
        if (version < MINIMAL_VERSION) {
            peer.disconnect();
            return;
        }
        long services = data.getLong();
        long timestamp = data.getLong();
        NetAddr receiver = NetAddr.fromBuffer(data, false);
        NetAddr sender = NetAddr.fromBuffer(data, false);
        long nonce = data.getLong();
        VarStr varStr = VarStr.fromBuffer(data);
        if (varStr == null) {
            peer.disconnect();
            return;
        }
        String userAgent = varStr.getString();
        int startHeight = data.getInt();
        byte doRelay = (byte)1;               // not present int the legacy wallet version message
        if ( data.position() < data.limit() )
            doRelay = data.get();
        boolean relay = ( doRelay == 1);
        // neni dodelany
        // peer je server - odpovedet version a versack
        // peer je klient -jen versack
        // predat parametry peerovi pro peerinfo
        peer.receiveRemoteInfo(startHeight, version);
        peer.sendMessage(Message.fromSource("verack", verack.fromSource(), peer));
        System.out.println("Remote block count " + startHeight);
    }

}

