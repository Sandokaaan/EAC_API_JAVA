package network;

import java.util.LinkedList;
import java.util.Queue;
import network.protocol.InvType;
import system.Task;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author virtu
 */
public class P2pClient  extends Task {
    private Peer peer;
    private final String ADDRESS;
    private final int PORT;
    private final int DELAY_RECONNECT;
    private boolean exiting;
    private final Queue<InvType> messages;
    private final SyncManager parent;
    
    public P2pClient(SyncManager parent){
        super("JAVA_P2P_CLIENT");
        this.ADDRESS = "127.0.0.1";   // config
        this.PORT = 35677;            // config
        this.DELAY_RECONNECT = 5000;
        peer = null;
        exiting = false;
        messages = new LinkedList<>();
        this.parent = parent;
    }

    @Override
    protected void initialization() {
        connect();
    }

    @Override
    protected void mainTask() {
        while (!exiting) {
            try {
                synchronized(messages) {
                    messages.wait();
                }
                dispatchEvent();
            } catch (InterruptedException ex) {
                exiting = true;
                System.err.printf(this.getName() + " was interrupted" );
            }
        }
    }

    @Override
    protected void finished() {
        disconnect();
    }
    
    private void connect() {
        if (peer != null && !exiting)
            peer.setExiting(true);
        peer = new Peer(ADDRESS, PORT, this);
        peer.start();
    }

    public void disconnect() {
        exiting = true;
        if (peer != null)
            peer.setExiting(true);
        peer = null;
    }
    
    private void dispatchEvent() throws InterruptedException {
        int blocks = 0;
        int errors = 0;
        synchronized(messages) {
            while (!messages.isEmpty()) {
                InvType msg = messages.poll();
                switch (msg) {
                    case MSG_BLOCK:
                        blocks++;
                        break;
                    case ERROR:
                        errors++;
                        break;
                }
            }
        }
        if (blocks>0) {
            System.out.println("A new block notified");
            parent.addBlockRequest(Integer.MAX_VALUE);   // queue requesting the most priority block
        }
        if (errors>0 && !exiting) {
            synchronized(messages) {
                System.err.println("P2P connection closed. Wait for reconnect...");
                messages.wait(DELAY_RECONNECT);
            }
            messages.clear();
            connect();
        }
    }
    
    public void addMessage(InvType msg) {
         synchronized(messages) {
             messages.add(msg);
             messages.notify();
         }         
    }
    
}
