/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.TreeSet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import system.Config;
import static system.Utils.setJsonOrdered;

/**
 *
 * @author Milan Gallas
 */
public class Database {
    public static final String BLOCKS = "blocks";
    public static final String TRANSACTIONS = "transactions";
    public static final String OUTPUTS = "outputs";
    public static final String ADDRESSES = "addresses";
    public static final String TXDETAILS = "txdetails";
    public static final String SPENT = "spent";    
    
    private static final Config config = Config.getConfig();
    private static final String JDBC_DRIVER = config.dbDriver;
    private static final String DB_URL_PREFIX = config.dbPrefix;
    private static final String USER = config.dbUser;
    private static final String PASS = config.dbPassword;     
    private Connection connection;
   
    /**
     * Open an embedded database at [current_path]/db/dbName
     * If not exists, create one.
     * @throws SQLException
     */
    public Database() throws SQLException{
       try {
           Class.forName(JDBC_DRIVER);
           connection = DriverManager.getConnection(DB_URL_PREFIX + config.dbName, USER, PASS);
       } catch (ClassNotFoundException ex) {
           throw new SQLException(ex);
       }
    }
    
    /*
    * Close the openned connection
    */
    public void close() {
        try {
            connection.close();
        } catch (SQLException ex) {
            System.err.println("Close of the database connection failed.");
        }        
    }
    
     /**
     * @param commands
     */
    public void command(String... commands) {
        try (Statement stmt = connection.createStatement()) {
           for (String sql : commands ) {
               stmt.addBatch(sql);
           }
           stmt.executeBatch();
       } catch (SQLException ex) {
           System.err.println(ex.getMessage());
       }
    }
    
    /**
     * Returns the count of rows in the table
     * @param table
     * @return
     */
    public int count(String table) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM " + table)) {
            try (ResultSet result = ps.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        } catch (SQLException ex) {
            return 0;
        }
    }
    
    public JSONArray select(String sql) {
        try {
            try (PreparedStatement ps = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
                try (ResultSet rs = ps.executeQuery()) {
                    return JSONArrayOf(rs);
                }
            }
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
            return new JSONArray();
        }        
    }
    
    public JSONObject selectOne(String sql) {
        try {
            try (PreparedStatement ps = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
                try (ResultSet rs = ps.executeQuery()) {
                    return JSONObjectOf(rs);
                }
            }
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
            return new JSONObject();
        }        
    }
    
    
    public TreeSet<Object> selectSet(String sql) {
        try {
            try (PreparedStatement ps = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
                try (ResultSet rs = ps.executeQuery()) {
                    return TreeSetOf(rs);
                }
            }
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
            return new TreeSet<>();
        }        
    }
    
    private TreeSet<Object> TreeSetOf(ResultSet rs){
        TreeSet<Object> rts = new TreeSet<>();
        try {
            while (rs.next()) {
                rts.add(rs.getObject(1));
            }
            return rts;
        } catch (SQLException ex) {            
            System.err.println("Database::TreeSetOf(JSONArray arr) - " + ex.getMessage());
            return rts;
        }
    }
    
    private String[] getColNames(ResultSet rs) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int cols = rsmd.getColumnCount();
        String[] colNames = new String[cols];
        for(int i=0; i<cols; i++)
            colNames[i] = rsmd.getColumnLabel(i+1).toLowerCase();  // to show alliasses
        return colNames;
            
    }
    
    private JSONArray JSONArrayOf(ResultSet rs){
        try {
            String[] colNames = getColNames(rs);
            int cols = colNames.length;
            JSONArray rts = new JSONArray();
            while (rs.next()) {
                JSONObject line = new JSONObject();
                setJsonOrdered(line);
                for (int i=0; i<cols; i++)
                    line.put(colNames[i], rs.getObject(i+1));
                rts.put(line);
            }
            return rts;
        } catch (SQLException | JSONException ex) {
            System.err.println("Database::JSONArrayOf(ResultSet rs) - " + ex.getMessage());
            return new JSONArray();
        }
    }
    
    private JSONObject JSONObjectOf(ResultSet rs){
        try {
            String[] colNames = getColNames(rs);
            int cols = colNames.length;
            if (rs.next()) {
                JSONObject line = new JSONObject();
                setJsonOrdered(line);
                for (int i=0; i<cols; i++)
                    line.put(colNames[i], rs.getObject(i+1));
                return line;
            }
            return new JSONObject();
        } catch (SQLException | JSONException ex) {
            System.err.println("Database::JSONArrayOf(ResultSet rs) - " + ex.getMessage());
            return new JSONObject();
        }        
    }
    
}