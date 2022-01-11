/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network;

import java.io.IOException;
import java.util.Base64;
import system.Utils;

/**
 *
 * @author virtu
 */
public class RpcClient {
    private final String authHash;
    private final String ip;
    private final int port;
    private String userAgent = "Java_Bitcoin_Client/1.0"; 

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public RpcClient(String ip, int port, String name, String password) {
        authHash = passwordHash(name, password);
        this.ip = ip;
        this.port = port;        
    }
    
    private String passwordHash(String name, String password) {
        String src = String.join(":", name, password);
        return Base64.getEncoder().encodeToString(src.getBytes(Utils.CHARSET));
    }
    
    private String prepareHeader(RpcRequest request) {
        String content = request.getAsString();
        String header = 
            "POST / HTTP/1.1\n" +
            "User-Agent: " + userAgent + "\n" +
            "Host: " + ip + ":" + port + "\n" + 
            "Content-Type: application/json\n" + 
            "Content-Length: " + content.length() + "\n" + 
            "Connection: close\n" + 
            "Accept: application/json\n" + 
            "Authorization: Basic " + authHash + "\n\n" +
            content + "\n";
        return header;
    }
    
    public RpcResponse query(RpcRequest request) throws IOException {
        String header = prepareHeader(request);            
        RawSocket rawSocket = new RawSocket();
        rawSocket.connect(ip, port);
        rawSocket.send(header);
        String rawRsponse = rawSocket.receivePacket(true).toString(Utils.CHARSETNAME);
        rawSocket.close();
        return RpcResponse.of(rawRsponse);
    }
    
    public RpcResponse query(String request) throws IOException {
        return query(RpcRequest.of(request));
    }

    public RpcResponse query(String request, Object... params) throws IOException {
        return query(RpcRequest.of(request, params));
    }
    
    
}
