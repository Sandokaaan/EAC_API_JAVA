/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package system;

import org.json.JSONObject;

/**
 * This class provides an internal representation of data structure returned
 * by a json server. Ii is used only as a temporary memory object in the
 * other network communication objects and methods, so constructor is private.
 * @author Sandokaaan 
 */
public final class JsonResponse {
    private final String response;  // internal storage for received data
    
    /**
     * The constructor is private to prevent an instance creation otherwise than
     * by the factory static method of().
     * @param response 
     */
    private JsonResponse(String response) {
        this.response = response;
    }

    /**
     * This method returns the received data as an unformated String object.
     * @return String
     */
    public String getAsString() {
        return response;
    }
    
    /**
     * This method returns the received data converted to a JSONObject.
     * @return JSONObject
     * If the String is empty or the conversion to JSON failed, returns null.
     */
    public JSONObject getAsJSON() throws org.json.JSONException {
        return new JSONObject(response);
    }
    
    /**
     * This is a static factory method to create an instance of Response object from a String.
     * @param string 
     * @return
     * It returs an instance of Response class with received data stored in it.
     * If network comunication failed, it returns null.
     */
    public final static JsonResponse of(String string) {
        return new JsonResponse(string);
    }
   
}
