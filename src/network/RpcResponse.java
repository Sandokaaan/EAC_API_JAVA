/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class provides an internal representation of data structure returned
 * by a json server. Ii is used only as a temporary memory object in the
 * other network communication objects and methods, so constructor is private.
 * @author Sandokaaan 
 */
public final class RpcResponse {
    private final int id;                  // rquest ID for verification
    private final JSONObject error;        // error, if applicable
    private final Object response;     // internal data structure of the response
    
    /**
     * The constructor is private to prevent an instance creation otherwise than
     * by the factory static method of().
     * @param response 
     */
    private RpcResponse(Object response, JSONObject error, int id) {
        this.response = response;
        this.error = error;
        this.id = id;
    }

    /**
     * This method returns the received data as an unformated String object.
     * @return String
     */
    public String getAsString() {
        return (response == null || response.equals(null)) ? error.toString() : response.toString();
    }
    
    public String getQuotedIfString(){
        String rts = null;
        try {
            if (response.getClass()== Class.forName("java.lang.String"))
                rts = "\"" + response + "\"";
        } catch (ClassNotFoundException ex) { }
        return (rts == null) ? getAsString() : rts;
    }
    
    
    /**
     * This method returns the received data as an Object.
     * @return String
     */
    public Object get() {
        return response;
    }
    
    public JSONObject getAsJson() {
        try {
            return (JSONObject) response;
        } catch (Exception Ex) {
            return new JSONObject();
        }
    }
    
    /**
     * This method returns the value of key if it is a valid JSON object.
     * @param key
     * @return String
     */
    public Object get(String key) {
        try {
            if (response.getClass() == Class.forName("org.json.JSONObject"))
                return ((org.json.JSONObject)response).get(key);
        } catch (ClassNotFoundException | JSONException ex) { }
        return null;
    }

    /**
     * This method returns possible error as a JSON object.
     * @return String
     */
    public JSONObject getError() {
        return error;
    }
        
    private static String removeHeader(String rawResponse) {
        String lines[] = rawResponse.split("\\r?\\n");
        int n = lines.length;
        return lines[n-1];
    }
    
    /**
     * This is a static factory method to create an instance of Response object from a String.
     * @param rawResponse - HTML response from the RPC server
     * @return
     * It returs an instance of Response class with received data stored in it.
     * If network comunication failed, it returns null.
     */
    public final static RpcResponse of(String rawResponse) {
        //System.err.println(rawResponse);
        JSONObject error = new JSONObject();
        Object response;
        JSONObject jsonResponse;
        int id = 0;
        try {
            jsonResponse = new JSONObject(removeHeader(rawResponse));
        } catch (org.json.JSONException ex) {
            error.put("error", ex.getMessage());
            return new RpcResponse(null, error, id);
        }
        id = jsonResponse.optInt("id");
        error.put("error", jsonResponse.optJSONObject("error"));
        response = jsonResponse.opt("result");
        return new RpcResponse(response, error, id);
    }
    
    public final static RpcResponse ofError(Exception ex) {
        JSONObject error = new JSONObject();
        error.put("error", ex.getMessage());
        return new RpcResponse(null, error, 0);
    }
   
}
