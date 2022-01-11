/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network;

import system.Task;
import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import network.protocol.InvType;
import system.Utils;
import network.protocol.Message;
import network.protocol.MessageHeader;
import network.protocol.Payload;
import network.protocol.messages.getaddr;
import network.protocol.messages.ping;
import network.protocol.messages.version;

/**
 *
 * @author virtu
 */
public class Peer extends Task {
    private final RawSocket rawSocket;
    private int pingDelay;
    private boolean exiting;
    private int versionSentTime;          // bude slouzit k odpojeni
    private int versionReceiveTime = 0;   // bude slouzit k odpojeni
    private final String remoteIP;
    private final int remotePort;
    private int remoteStartBlock;
    private final int localStartBlock;
    private int remoteVersion;
    private int status;
    private static boolean askedMore = false;
    private final boolean incoming;
    private int requestsSent;
    private int answersReceiwed;
    private boolean headerResponseWait;
    private final P2pClient parent;

    public boolean isHeaderResponseWait() {
        return headerResponseWait;
    }

    public void setHeaderResponseWait(boolean headerResponseWait) {
        this.headerResponseWait = headerResponseWait;
    }    
    
    public void setExiting(boolean exiting) {
        this.exiting = exiting;
        try {
            rawSocket.close();
        } catch (IOException ex) {
            System.err.println("Socket can not be closed.");
        }
    }

    public boolean isExiting() {
        return exiting;
    }

    public int getRequestsSent() {
        return requestsSent;
    }

    public int getAnswersReceiwed() {
        return answersReceiwed;
    }
    
    public void incAnswers() {
        answersReceiwed++;
    }
    
    public void addRequestCount(int cnt) {
        requestsSent += cnt;
    }
    
    public Peer(String ip, int port, P2pClient parent) {
        super("JAVA_P2P_PEER_CONNECTION_AT_" + ip + "_PORT_" + port);
        this.parent = parent;
        incoming = false;
        status = 0;
        localStartBlock = 0;    
        remoteStartBlock = 0;
        remoteIP = ip;
        remotePort = port;
        pingDelay = -1;
        exiting = false;
        rawSocket = new RawSocket();
        requestsSent = 0;
        answersReceiwed = 0;
        headerResponseWait = false;
    }
    
    @Override
    protected void initialization() {
        connect();
    }

    public int getStatus() {
        return status;
    }

    private void connect() {
        if (rawSocket != null) try {
            if (!incoming) {
                rawSocket.connect(remoteIP, remotePort);
                status = 1;
            }
            Payload pld = version.fromSource(remoteIP, remotePort, rawSocket.getLocalIp(), rawSocket.getLocalPort(), localStartBlock, true);
            Message msg = Message.fromSource("version", pld, this);
            rawSocket.sendP2PMessage(msg);
            versionSentTime = (int) Utils.timeNow();
        } catch (IOException ex) {
            parent.addMessage(InvType.ERROR);
            System.err.println("Error: " + ex.getMessage());
            status = -1;
            exiting = true;
        }        
    }

    public String getRemoteIP() {
        return remoteIP;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public int getRemoteStartBlock() {
        return remoteStartBlock;
    }

    public int getRemoteVersion() {
        return remoteVersion;
    }
    
    public final int getPingDelay() {
        return pingDelay;
    }
    
    public final void receivePong(int pingDelay) {
        this.pingDelay = pingDelay;
        status = 3;
        if (!askedMore) {
            askedMore = true;
            askMoreAddrs();
        }
    }
    
    private void askMoreAddrs() {
        // ignore - no need for more peers for this simple usage
        /*
        Payload pld = getaddr.fromSource();
        Message msg = Message.fromSource("getaddr", pld, this);
        sendMessage(msg);
        System.err.println("ASK FOR MORE sent.");*/
    }
    
    public void receivedVerack(int time) {
        status = 2;
        versionReceiveTime = time;
        Payload pld = ping.fromSource();
        Message pingmes = Message.fromSource("ping", pld, this);
        sendMessage(pingmes);
    }
    
    public final void sendMessage(Message msg) {
        byte[] debug = msg.array();
        System.out.println("Peer.sendMessage " + msg.getMessageType() + " <-- " + debug.length ); 
        msg.setPeer(this);
        try {
            if (!(rawSocket.isOpen()))
                throw new IOException("Socket is not open.");
            rawSocket.sendP2PMessage(msg);           
        } catch (IOException ex) {
            parent.addMessage(InvType.ERROR);
            disconnect();
        }
    }
    
    public final void disconnect() {
        exiting = true;
        status = -2;
        try {
            if (rawSocket.isOpen())
                rawSocket.close();
        } catch (IOException ex) {
            System.err.println("Error: " + ex.getMessage());
        }
    }
    
    // toto by se melo presunout do RawSocket ? patri to tam logicky?
    private void readP2PPacket() {
        try {
            //System.err.println(" *** be here *** ");
            ByteBuffer headerBuf = rawSocket.receiveBytes(24);           // header_size = 4 + 12 + 4 + 4
            if (headerBuf == null) { 
                throw new IOException("Invalid packet header.");
            }
            MessageHeader msgHead = MessageHeader.fromBuffer(headerBuf);
            if (msgHead == null)
                throw new IOException("Invalid magic in packet header.");
            String command = msgHead.getCommand();
            ByteBuffer tmp = rawSocket.receiveBytes(msgHead.getPayloadLength());
            if ( (tmp == null) || (msgHead.getPayloadChecksum() != system.Utils.checksum(tmp.array())) )
                throw new IOException("Payload receiving failed.");
            Message msg = Message.fromSource(command, Payload.ofName(command, tmp), this);
            if (msg != null)
                msg.dispatch();
        } catch (IOException ex) {
            System.err.println("Error in network packet read: " + ex.getMessage());
            parent.addMessage(InvType.ERROR);
            disconnect();
        }   
    }
    
    /**
    * This method try to receive a P2P packec from the socket and push it into 
    * the message queue for dispatch.
    */
    @Override
    protected void mainTask() {        
        while (!exiting)
            readP2PPacket();
    }
    
    public final void checkConnection() {
        requestsSent = 0;
        answersReceiwed = 0;
        headerResponseWait = false;
        pingDelay = -1;
        status = 2;
        sendMessage(Message.fromSource("ping", ping.fromSource(), this));
    }
    
    public final void askPeers() {
        sendMessage(Message.fromSource("getaddr", getaddr.fromSource(), null));
    }
    
    public void receiveRemoteInfo(int remoteStartBlock, int remoteVersion) {
        try {
            rawSocket.setTimeOut(1000*60*30);       // ping every 30 minutes -- CONFIG
        } catch (SocketException ex) {
            parent.addMessage(InvType.ERROR);
            disconnect();
            return;
        }
        this.remoteStartBlock = remoteStartBlock;
        this.remoteVersion = remoteVersion;
    }

    @Override
    protected void finished() {
        exiting = true;        
    }
    
    public final P2pClient getParent() {
        return this.parent;
    }
    
}
