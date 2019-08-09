/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package system;

import java.util.Base64;
import org.json.JSONObject;

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
        return Base64.getEncoder(). encodeToString(src.getBytes(Utils.CHARSET));
    }
    
    private String prepareHeader(JsonRequest request) {
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
    
    private String removeHeader(String response) {
        // for now no need for verify header
        String lines[] = response.split("\\r?\\n");
        int n = lines.length;
        return lines[n-1];
    }
    
    private String removeEnvelope(JsonResponse jsonResponse, int id) {
        JSONObject jo = null; 
        if (jsonResponse != null) {
            jo = jsonResponse.getAsJSON();
        }
        if (jo == null) {
            return ("{\"error\": {\"code\":-404,\"message\":\"Communication protocol fault\"}}");
        }
        try {
            if (!(jo.get("id").equals(id))) {
                return ("{\"error\": {\"code\":-404,\"message\":\"Invalid response checksum\"}}");
            }
            if (!(jo.get("error").equals(null))) {
                return "{\"error\": " + jo.get("error").toString()+"}";
            }
            return jo.get("result").toString();
        } catch (org.json.JSONException ex) {
            return ("{\"error\": {\"code\":-404,\"message\":\"Response decoding fault\"}}");
        }
    }   
    
    public String query(String method, Object... objects) {
        try {
            JsonRequest request = JsonRequest.of(method, objects);
            String header = prepareHeader(request);
            RawSocket rawSocket = new RawSocket();
            rawSocket.connect(ip, port);
            rawSocket.send(header);
            String response = rawSocket.receivePacket(true).toString(Utils.CHARSETNAME);
            rawSocket.close();
            JsonResponse jsonResponse = JsonResponse.of(removeHeader(response));
            return removeEnvelope(jsonResponse, request.getId());
        } catch (java.io.IOException ex) {
            return ("{\"error\":\"network communication\",\"message\":\"connection fault\"}");
        } catch (org.json.JSONException ex) {
            System.err.println(ex.getMessage());
            return ("{\"error\":\"network communication\",\"message\":\"authentization fault\"}");
        }        
    }
    
    
}
