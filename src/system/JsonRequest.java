/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package system;

import org.json.JSONObject;

/**
 * This class provides an internal representation of data structure to be sent
 * to a bitcoin rpc server. Is used only as a temporary memory object in the
 * network communication of client and the rpc server.
 * @author Sandokaaan
 */
public final class JsonRequest {
    private final JSONObject jo;    // internal data structure of the query
    private final int id;   // a random number for verification of the response

    /**
     * Returns the stored random number, used for verification of the response
     * @return int
     */
    public int getId() {
        return id;
    }

    /**
     * The constructor is private to prevent an instance creation otherwise than
     * by the factory static method of().
     * @param jo org.json.JSONObject
     * @param id int
     */
    private JsonRequest(final String method, final Object... params) throws org.json.JSONException {
        this.id = Utils.RANDOM.nextInt();
        jo = new JSONObject().put("method", method).put("params", params).put("id", id);
    }
    
    /**
     * This method creates a read-only copy of the internal data and returs them as String.
     * @return String
     * If no data are stored, returns null.
     */
    public final String getAsString() {
        return jo.toString();
    }
    
    /**
     * This method creates a read-only copy of the internal data and returs them as byte array.
     * @return byte[]
     * If no data are stored, returns null.
     */
    public final byte[] getAsBytes() {
        return jo.toString().getBytes(Utils.CHARSET);
    }
        
    /**
     * A static factory method to create an instance of Request object.
     * This method also generate the id number used for the response check.
     * @param method String
     * @param params one or more Objects, also no Object as an empty array
     * Examples:
     * - Request.of("getinfo", []);
     * - Request.of("getblockhash", 1);
     * - Request.of("getblock", 0000000043a35ba235854d324ec48acc20201d61c10b54287f90e4381237c5f9, false);
     * @return
     * Returs an instance of Request with encoded method name an parameters in it.
     * If the parse of parameters to JSON failed, it returs null.
     */
    public static JsonRequest of(final String method, final Object... params) {
        try {
            return new JsonRequest(method, params);
        } catch (org.json.JSONException ex) {
            return null;
        }
    }
}

