/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package apiserver;

import apiextension.Config;
import java.io.IOException;
import java.net.ServerSocket;
import system.RawSocket;
import system.RpcClient;

/**
 *
 * @author Vratislav Bednařík
 */
public class ApiListen extends system.Task {
    private static final String DEFAULT_THREAD_NAME = "API listen for incoming connections";
    private boolean exiting;
    private ServerSocket serverSocket;
    RpcClient client;
           
    public ApiListen() {
        super(DEFAULT_THREAD_NAME);
        Config config = Config.getConfig();
        exiting = true;
        serverSocket = null;
        client = new RpcClient(config.rpcaddr, config.rpcport, config.username, config.password);
    }

    @Override
    protected void initialization() {
        Config config = Config.getConfig();
        try {
            serverSocket = new ServerSocket(config.apiport);
            exiting = false;
        } catch (IOException ex) {
            System.err.println("Open API port failed. " + ex.getMessage());
        }
    }

    @Override
    protected void mainTask() {
        while (!exiting) {
            try {
                RawSocket rawSocket = new RawSocket(serverSocket.accept());
            ApiResponse responder = new ApiResponse(rawSocket, client);
                responder.start();
            } catch (IOException ex) {
                System.err.println("Accept API connection failed. " + ex.getMessage());
            }
        }
    }

    @Override
    protected void finished() {
        try {
            if (!serverSocket.isClosed())
            serverSocket.close();
        } catch (IOException ex) {
            System.err.println("Close API port failed. " + ex.getMessage());
        }
        exiting = true;
    }

    public void close() {
        exiting = true;
        try {
            if (!serverSocket.isClosed())
                serverSocket.close();
        } catch (IOException ex) {
            System.err.println("Close API port failed. " + ex.getMessage());
        }
    }
    
}
