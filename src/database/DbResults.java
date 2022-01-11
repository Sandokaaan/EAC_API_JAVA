/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database;

import java.sql.ResultSet;
import org.json.JSONArray;
import org.json.JSONObject;
import java.sql.ResultSetMetaData;

import java.util.TreeSet;

/**
 *
 * @author virtu
 */
public class DbResults {
    
    @SuppressWarnings({"UseSpecificCatch", "ConvertToTryWithResources"})
    public static JSONArray JSONArrayOf(ResultSet rs){
        try {
            ResultSetMetaData rsmd = rs.getMetaData();
            int cols = rsmd.getColumnCount();
            String[] colNames = new String[cols];
            for(int i=0; i<cols; i++)
                colNames[i] = rsmd.getColumnLabel(i+1).toLowerCase();  // to show alliasses 
            JSONArray rts = new JSONArray();
            while (rs.next()) {
                JSONObject line = new JSONObject();
                for (int i=0; i<cols; i++)
                    line.put(colNames[i], rs.getObject(i+1));
                rts.put(line);
            }
            rs.close();
            return rts;
        } catch (Exception ex) {
            //System.err.println("DbResults::JSONArrayOf(ResultSet rs) - " + ex.getMessage());
            return new JSONArray();
        }
    }
    
    @SuppressWarnings("UseSpecificCatch")
    public static JSONObject JSONObjectOf(JSONArray arr){
        try {
            return arr.getJSONObject(0);
        } catch (Exception ex) {
            //System.err.println("DbResults::JSONObjectOf(JSONArray arr) - " + ex.getMessage());
            return new JSONObject();
        }
    }
    
    @SuppressWarnings({"UseSpecificCatch", "ConvertToTryWithResources"})
    public static TreeSet<Object> TreeSetOf(ResultSet rs){
        TreeSet<Object> rts = new TreeSet<>();
        try {
            while (rs.next()) {
                rts.add(rs.getObject(1));
            }
            rs.close();
            return rts;
        } catch (Exception ex) {            
            return rts;
        }
    }
    
}
