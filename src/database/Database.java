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
    private static final Config config = Config.getConfig();
    private static final String JDBC_DRIVER = config.dbDriver;
    private static final String DB_URL_PREFIX = config.dbPrefix;
    private static final String USER = config.dbUser;
    private static final String PASS = config.dbPassword;     
    private Connection connection;
   
    /*
    * Close all connection and defrag the database.
    * Should be calles only once at the program exit.
    */
    public static void closeAllConnections(String dbName) {
        try {
           Database db = new Database();
           System.out.println("Please wait, maintaining database...");
           db.command("SHUTDOWN DEFRAG");
           System.out.println("Database closed.");
        } catch (SQLException ex) {
           System.err.println("Database maintaince failed.");
        }
    }
   
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
     * @param sql
     * @return 
     */
    public int command(String sql) {
        try (Statement stmt = connection.createStatement()){
           return stmt.executeUpdate(sql);
       } catch (SQLException ex) {
           System.err.println(ex.getMessage());
           return -1;
       }
    }
    
    public int commandWithParams(String sql, Object[] params) {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int index = 1;
            for(Object param : params) 
                ps.setObject(index++, param);
            return ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
            return -1;
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
    
     /**
     * If the table does not exist, try create it. 
     * @param tableName
     * @param items
     * @throws SQLException 
     * Example:
     *              database.createTable("tableName", 
                        "id INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY",
                        "jmeno VARCHAR( 30 ) NOT NULL",
                        "vek INT NOT NULL",
                        "jazyk VARCHAR( 20 ) NOT NULL" );
     */
    public void createTable(String tableName, String...items) throws SQLException {
        String params = String.join(", ", items);
        String sql = String.format("CREATE TABLE IF NOT EXISTS %s (%s)", tableName, params);
        try (Statement stmt = connection.createStatement()) {
           stmt.executeUpdate(sql);
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
        //System.out.println(sql);
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