/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database;

import java.sql.SQLException;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import static system.Utils.setJsonOrdered;

/**
 *
 * @author virtu
 */
public class DbReader implements AutoCloseable{
    

    private Database db = null; 
    
    /*
    * Constructor - open a new connection to the database
    */    
    public DbReader() throws SQLException {
        db = new Database();
    }
    
    /*
    * Close the connection to the database
    * The creator JSONArrayOf DbReader object must call close on finishing
    */
    @Override
    public final void close() {
        db.close();
    }

    
/*
* Section - methods for blocks
*/

    
    public int getBlockCount() {
        return db.count(Database.BLOCKS);
    }
    
    public JSONObject getBlock(String hash) {
        return selectFirstAsJSON(
            "SELECT * FROM %s WHERE hash='%s' LIMIT 1; ", 
            Database.BLOCKS, hash
        );
    }    
    
    public JSONObject getBlock(int height) {
        return selectFirstAsJSON(
            "SELECT * FROM %s WHERE height=%d LIMIT 1; ", 
            Database.BLOCKS, height
        );
    }
    
    public JSONObject getBestBlock() {
        return selectFirstAsJSON(
            "SELECT hash FROM %s WHERE height=(SELECT MAX(height) FROM %1$s) LIMIT 1; ", 
            Database.BLOCKS
        );        
    }

    public int getBestHeight() {
        return selectFirstAsJSON(
            "SELECT MAX(height) AS height FROM %s; ", 
            Database.BLOCKS
        ).optInt("height", -1);          
    }

    public int getLowestHeight() {
        return selectFirstAsJSON(
            "SELECT MIN(height) AS height FROM %s; ", 
            Database.BLOCKS
        ).optInt("height", -1);          
    }
    
    public String getBestHash() {
        return getBestBlock().optString("hash", "");
    }

    public String getBlockHash(int height) {
        return getBlock(height).optString("hash", "");
    }
    
    public int getBlockHeight(String hash) {
        return getBlock(hash).optInt("height", -1);
    }
    
    public TreeSet<Object> getAllHeights() {
        return db.selectSet(String.format(
            "SELECT height FROM %s ;",
            Database.BLOCKS
        ));
    }
            

/*
* Section - methods for transactions
*/
   
    
    public int getTransactionCount() {
        return db.count(Database.TRANSACTIONS);
    }

    public TreeSet<Object> getMostFreshHeights() {
        return db.selectSet(String.format(
            "SELECT height FROM %s WHERE tx_id>(SELECT MAX(tx_id)-500 FROM %1$s );",
            Database.TXDETAILS
        ));        
    }
    
    
/*
* Section JSONArrayOf private supporting methods    
*/
    
    private JSONArray selectAsJSON(String query, Object...objects){
        return db.select(String.format(query, objects));
    }
    
    public JSONArray selectAsJSON(String query){
        return db.select(query);
    }
    
    private JSONObject selectFirstAsJSON(String query, Object...objects){
        return db.selectOne(String.format(query, objects));
    }

    
    /*
    * Section complex queries
    */
    
    public JSONObject getSyncStat() {
        return selectFirstAsJSON(
            "SELECT "
                + "CONVERT( 100*COUNT(*)/(1.0+MAX(height)), REAL ) AS sync, " 
                + "COUNT(*) AS count, "
                + "MAX(height) AS height, "
                + "UNIX_TIMESTAMP() - MAX(time) AS age "
            + "FROM " + Database.BLOCKS + ";"
        );
    }
    
    private JSONArray getSentHistory(String address, int limit, int bestHeight) {
        //System.out.println("sent");
        return selectAsJSON(
            "SELECT " +
                "'sent' AS type, " +
                "txid, " +
                "hash, " +
                "height, " +
                "(" + bestHeight + " - height)  AS confirmations, " +
                "time, "  +   
                "txcomment, " +
                "ipfs, " +
                "vout, " +
                "address, " + 
                "value " +    
            "FROM " + Database.TRANSACTIONS +
            " NATURAL JOIN " + Database.TXDETAILS +
            " NATURAL JOIN " + Database.BLOCKS +   
            " NATURAL JOIN " + Database.OUTPUTS +         
            " NATURAL JOIN " + Database.ADDRESSES +
            " WHERE tx_id IN (" +
                    "SELECT spending_tx_id AS tx_id FROM " + Database.SPENT +
                    " NATURAL JOIN " + Database.OUTPUTS +
                    " NATURAL JOIN " + Database.ADDRESSES +
                    " WHERE address='" + address + "')" +
            " ORDER BY height DESC, txid, vout ASC" +                        
            " LIMIT " + limit + ";"   
        );
    }
    
    private JSONArray getReceivedHistory(String address, int limit, int bestHeight) {
        //System.out.println("received");
        return selectAsJSON(
            "SELECT " +
                "CASE WHEN coinbase=true THEN 'mined' ELSE 'received' END AS type, " +
                "'" + address + "' AS address, " +
                "(SELECT address FROM " + Database.SPENT + " AS s " +
                    "LEFT JOIN " + Database.OUTPUTS + " AS o " +
                        "ON o.tx_id=s.tx_id " +
                    "LEFT JOIN " + Database.ADDRESSES + " AS a " +
                        "ON a.addr_id=o.addr_id " +
                    "WHERE s.spending_tx_id=f.tx_id " +
                    "LIMIT 1 " +
                ") AS sender, " +
                "value, " +
                "CASE WHEN spending_tx_id IS NULL THEN false ELSE true END AS spent," +    
                "txid, " +
                "f.vout, " +
                "time, " +
                "height, " +
                "(" + bestHeight + " - height)  AS confirmations, " +         
                "hash, " +
                "txcomment, " +
                "ipfs " +    
            "FROM (" +
                "SELECT tx_id, vout " +
                "FROM " + Database.OUTPUTS + 
                " NATURAL JOIN " + Database.TXDETAILS +    
                " NATURAL JOIN " + Database.ADDRESSES +    
                    " WHERE address='" + address + "'" +
                    " ORDER BY height DESC" +
                    " LIMIT " + limit +          
            ") AS f" +
            " NATURAL JOIN " + Database.TRANSACTIONS +
            " NATURAL JOIN " + Database.TXDETAILS +
            " NATURAL JOIN " + Database.BLOCKS +
            " NATURAL JOIN " + Database.OUTPUTS +        
            " LEFT JOIN " + Database.SPENT + " AS s " +
                "ON f.tx_id=s.tx_id AND f.vout=s.vout; "
        );
    }
    
    
    private JSONArray rearrange(JSONArray received, JSONArray sent){
        //System.out.println("rearrange");
        JSONArray rts = new JSONArray();
        try {
            received.forEach((Object item) -> {
                rts.put(item);
            });
            JSONObject tx = new JSONObject();
            setJsonOrdered(tx);
            JSONArray outputs = new JSONArray();
            String lastTxid = "";
            for (Object o : sent) {
                JSONObject jsonItem = (JSONObject)o;
                String txid = jsonItem.optString("txid", "");
                if (txid.length()>0 && !(lastTxid.equals(txid))) {
                    if (outputs.length()>0) {
                        tx.put("outputs", outputs);
                        rts.put(tx);
                        outputs = new JSONArray();
                    }
                    tx = new JSONObject();
                    setJsonOrdered(tx);
                    lastTxid = txid;
                    Object[] items = Stream
                        .of("type", "txid", "time", "height", "confirmations", "hash", "txcomment", "ipfs")
                        .filter(jsonItem::has).toArray();
                    for (Object item : items) {
                        String key = (String)item;
                        tx.put(key, jsonItem.get(key));
                    }                        
                }
                Object[] items = {"address", "value", "vout"};
                JSONObject output = new JSONObject();
                    setJsonOrdered(output);
                for (Object item : items) {
                    String key = (String)item;
                    output.put(key, jsonItem.get(key));
                }
                outputs.put(output);                
            }
            if (outputs.length()>0) {
                tx.put("outputs", outputs);
                rts.put(tx);
            }
        } catch(JSONException ex) {
            // log it or do nothing
            System.err.println("Error - rearrange " + ex.getMessage());
        }
        return rts;
    }  
    
    private JSONObject addressStatReceived(String address) {
        return selectFirstAsJSON(
            "SELECT " +
                "COUNT(coinbase) AS mined, " +
                "COUNT(tx_id) AS inputs, " +
                "SUM(value) AS total_value, " +
                "SUM(CASE WHEN coinbase=true THEN 0 ELSE value END) AS received_value, " +
                "MIN(height) AS low_block, " +
                "MAX(height) AS high_block, " +
                "COUNT(ipfs) AS with_ipfs, " +
                "COUNT(txcomment) AS with_txcomment, " +
                "MIN(time) AS low_time, " +     
                "MAX(time) AS high_time " +
            "FROM " + Database.OUTPUTS +
            " NATURAL JOIN " + Database.TXDETAILS +
            " NATURAL JOIN " + Database.BLOCKS +
            " NATURAL JOIN " + Database.ADDRESSES +        
            " WHERE address='" + address + "';"        
        );
    }

    private JSONObject addressStatSent(String address) {
        return selectFirstAsJSON(
            "SELECT " +
                "COUNT(spending_tx_id) AS outputs, " +
                "COUNT(distinct spending_tx_id) AS transactions, " +
                "SUM(value) AS total_value, " +
                "MIN(d.height) AS low_block, " +
                "MAX(d.height) AS high_block, " +
                "COUNT(ipfs) AS with_ipfs, " +
                "COUNT(txcomment) AS with_txcomment, " +
                "MIN(time) AS low_time, " +     
                "MAX(time) AS high_time " +
            "FROM (SELECT '" +
                address + "' AS address) AS param " +
            "NATURAL JOIN " + Database.ADDRESSES + " AS a " +
            "NATURAL JOIN " + Database.OUTPUTS + " AS o " +   
            "NATURAL JOIN " + Database.SPENT + " AS s " +        
            "INNER JOIN " + Database.TXDETAILS + " AS d " +
                "ON s.spending_tx_id=d.tx_id " +
            "NATURAL JOIN " + Database.BLOCKS + " AS b; "        
        );
    }    
    
    public JSONObject addressStatistics(String address) {
        return selectFirstAsJSON(
            "SELECT " +
                "'" + address + "' AS address, " +
                "(total_input_value-sent_value) AS balance, " +
                "(inputs_count + output_transactions_count - mined_count) AS transactions, " +
                "(inputs_count - mined_count) AS received, " +
                "output_transactions_count AS sent, " +
                "mined_count AS mined, " +
                "(total_input_value - mined_value) AS received_value, " +
                "sent_value, " +
                "mined_value, " +
                "low_block, " +
                "high_block, " +
                "(SELECT time FROM " + Database.BLOCKS + " WHERE height=low_block) AS low_time, " +
                "(SELECT time FROM " + Database.BLOCKS + " WHERE height=high_block) AS high_time, " +
                "received_txcomment, " +
                "received_ipfs, " +
                "sent_txcomment, " +
                "sent_ipfs " +
            "FROM (SELECT " +
                    "SUM(CASE WHEN it.coinbase=false THEN 0 ELSE 1 END) AS mined_count, " +
                    "COUNT(f.tx_id) AS inputs_count, " +
                    "COUNT(DISTINCT spending_tx_id) AS output_transactions_count, " +
                    "SUM(value) AS total_input_value, " +
                    "SUM(CASE WHEN it.coinbase=false THEN 0 ELSE value END) AS mined_value, " +
                    "SUM(CASE WHEN spending_tx_id IS NULL THEN 0 ELSE value END) AS sent_value, " +
                    "LEAST(MIN(it.height),COALESCE(MIN(ot.height),2147483647)) AS low_block, " +
                    "GREATEST(MAX(it.height),COALESCE(MAX(ot.height),0)) AS high_block, " +
                    "COUNT(DISTINCT it.ipfs) AS received_ipfs, " +
                    "COUNT(DISTINCT ot.ipfs) AS sent_ipfs, " +
                    "COUNT(DISTINCT it.txcomment) AS received_txcomment, " +
                    "COUNT(DISTINCT ot.txcomment) AS sent_txcomment " +
                "FROM (SELECT tx_id, vout, value FROM " + Database.OUTPUTS + " AS o " +
                "NATURAL JOIN " + Database.ADDRESSES +
                " WHERE address='" + address + "') AS f " +
            "LEFT JOIN " + Database.TXDETAILS + " AS it " +
                "ON it.tx_id=f.tx_id " +
            "LEFT JOIN " + Database.SPENT  + " AS s " +
                "ON s.tx_id=f.tx_id AND s.vout=f.vout " +
            "LEFT JOIN " + Database.TXDETAILS + " AS ot " +
                "ON ot.tx_id=s.spending_tx_id) AS sumary; "
        );        
    }
    
    private JSONArray getReceivedByHeight(String address, int lowBlock, int highBlock, boolean received, boolean mined, int bestHeight) {
        boolean both = received && mined;
        return selectAsJSON(
            "SELECT " +
                "CASE WHEN coinbase=true THEN 'mined' ELSE 'received' END AS type, " +
                "'" + address + "' AS address, " +
                "(SELECT address FROM " + Database.SPENT + " AS s " +
                    "LEFT JOIN " + Database.OUTPUTS + " AS o " +
                        "ON o.tx_id=s.tx_id " +
                    "LEFT JOIN " + Database.ADDRESSES + " AS a " +
                        "ON a.addr_id=o.addr_id " +
                    "WHERE s.spending_tx_id=f.tx_id " +
                    "LIMIT 1 " +
                ") AS sender, " +                        
                "value, " +
                "CASE WHEN spending_tx_id IS NULL THEN false ELSE true END AS spent," +    
                "txid, " +
                "f.vout, " +
                "time, " +
                "height, " +
                "(" + bestHeight + " - height)  AS confirmations, " +
                "hash, " +
                "txcomment, " +
                "ipfs " +    
            "FROM (" +
                "SELECT tx_id, vout " +
                "FROM " + Database.OUTPUTS + 
                " NATURAL JOIN " + Database.TXDETAILS +    
                " NATURAL JOIN " + Database.ADDRESSES +    
                    " WHERE address='" + address + "'" +
                    " AND height BETWEEN " + lowBlock + " AND " + highBlock +
                    ((both) ? "" : " AND coinbase=" + (mined ? "TRUE": "FALSE")) +
                    " ORDER BY height DESC" +
                    " LIMIT 100" +          
            ") AS f" +
            " NATURAL JOIN " + Database.TRANSACTIONS +
            " NATURAL JOIN " + Database.TXDETAILS +
            " NATURAL JOIN " + Database.BLOCKS +
            " NATURAL JOIN " + Database.OUTPUTS +        
            " LEFT JOIN " + Database.SPENT + " AS s " +
                "ON f.tx_id=s.tx_id AND f.vout=s.vout; "
        );
    }
    
    private JSONArray getReceivedByTime(String address, int lowTime, int highTime, boolean received, boolean mined, int bestHeight) {
        boolean both = received && mined;
        return selectAsJSON(
            "SELECT " +
                "CASE WHEN coinbase=true THEN 'mined' ELSE 'received' END AS type, " +
                "'" + address + "' AS address, " +
                "(SELECT address FROM " + Database.SPENT + " AS s " +
                    "LEFT JOIN " + Database.OUTPUTS + " AS o " +
                        "ON o.tx_id=s.tx_id " +
                    "LEFT JOIN " + Database.ADDRESSES + " AS a " +
                        "ON a.addr_id=o.addr_id " +
                    "WHERE s.spending_tx_id=f.tx_id " +
                    "LIMIT 1 " +
                ") AS sender, " +                        
                "value, " +
                "CASE WHEN spending_tx_id IS NULL THEN false ELSE true END AS spent," +    
                "txid, " +
                "f.vout, " +
                "time, " +
                "height, " +
                "(" + bestHeight + " - height)  AS confirmations, " +
                "hash, " +
                "txcomment, " +
                "ipfs " +    
            "FROM (" +
                "SELECT tx_id, vout " +
                "FROM " + Database.OUTPUTS + 
                " NATURAL JOIN " + Database.TXDETAILS +  
                " NATURAL JOIN " + Database.BLOCKS +
                " NATURAL JOIN " + Database.ADDRESSES +    
                    " WHERE address='" + address + "'" +
                    " AND time BETWEEN " + lowTime + " AND " + highTime +
                    ((both) ? "" : " AND coinbase=" + (mined ? "TRUE": "FALSE")) +
                    " ORDER BY height DESC" +
                    " LIMIT 100" +          
            ") AS f" +
            " NATURAL JOIN " + Database.TRANSACTIONS +
            " NATURAL JOIN " + Database.TXDETAILS +
            " NATURAL JOIN " + Database.BLOCKS +
            " NATURAL JOIN " + Database.OUTPUTS +        
            " LEFT JOIN " + Database.SPENT + " AS s " +
                "ON f.tx_id=s.tx_id AND f.vout=s.vout; "
        );
    } 
    
    private JSONArray getSentHistoryByTime(String address, int lowTime, int highTime, int bestHeight) {
        //System.out.println("sent");
        return selectAsJSON(
            "SELECT " +
                "'sent' AS type, " +
                "txid, " +
                "hash, " +
                "height, " +
                "(" + bestHeight + " - height)  AS confirmations, " +
                "time, "  +   
                "txcomment, " +
                "ipfs, " +
                "vout, " +
                "address, " + 
                "value " +    
            "FROM " + Database.TRANSACTIONS +
            " NATURAL JOIN " + Database.TXDETAILS +
            " NATURAL JOIN " + Database.BLOCKS +   
            " NATURAL JOIN " + Database.OUTPUTS +         
            " NATURAL JOIN " + Database.ADDRESSES +
            " WHERE tx_id IN (" +
                    "SELECT spending_tx_id AS tx_id FROM " + Database.SPENT +
                    " NATURAL JOIN " + Database.OUTPUTS +
                    " NATURAL JOIN " + Database.ADDRESSES +
                    " WHERE address='" + address + "')" +
            " AND time BETWEEN " + lowTime + " AND " + highTime +
            " ORDER BY height DESC, txid, vout ASC" +                        
            " LIMIT 100;"   
        );    
    }
    
    private JSONArray getSentHistoryByHeight(String address, int lowBlock, int highBlock, int bestHeight) {
        //System.out.println("sent");
        return selectAsJSON(
            "SELECT " +
                "'sent' AS type, " +
                "txid, " +
                "hash, " +
                "height, " +
                "(" + bestHeight + " - height)  AS confirmations, " +
                "time, "  +   
                "txcomment, " +
                "ipfs, " +
                "vout, " +
                "address, " + 
                "value " +    
            "FROM " + Database.TRANSACTIONS +
            " NATURAL JOIN " + Database.TXDETAILS +
            " NATURAL JOIN " + Database.BLOCKS +   
            " NATURAL JOIN " + Database.OUTPUTS +         
            " NATURAL JOIN " + Database.ADDRESSES +
            " WHERE tx_id IN (" +
                    "SELECT spending_tx_id AS tx_id FROM " + Database.SPENT +
                    " NATURAL JOIN " + Database.OUTPUTS +
                    " NATURAL JOIN " + Database.ADDRESSES +
                    " WHERE address='" + address + "')" +
            " AND height BETWEEN " + lowBlock + " AND " + highBlock +
            " ORDER BY height DESC, txid, vout ASC" +                        
            " LIMIT 100;"   
        );    
    }    

    public int getHighestTime() {
        return getBestBlock().optInt("time", Integer.MAX_VALUE);
    }    

    public int getLowestTime() {
        return getBlock(0).optInt("time", Integer.MIN_VALUE);
    }

    public String getHistorry(String address, int limitR, int limitS) {
        int bestHeight = getBestHeight();
        JSONArray received = getReceivedHistory(address, limitR, bestHeight);
        JSONArray sent = getSentHistory(address, limitS, bestHeight);
        return rearrange(received, sent).toString();
    }

    
    public String getHistorryByHeight(String address, int bitmask, int lowBlock, int highBlock) {
        int bestHeight = getBestHeight();
        JSONArray received = ((bitmask & 5) != 0) ? 
                getReceivedByHeight(address, lowBlock, highBlock, ((bitmask & 1) != 0), ((bitmask & 4) != 0), bestHeight) 
                : new JSONArray();
        JSONArray sent = ((bitmask & 2) != 0) ? getSentHistoryByHeight(address, lowBlock, highBlock, bestHeight) : new JSONArray();
        return rearrange(received, sent).toString();
    }
    
    public String getHistorryByTime(String address, int bitmask, int lowTime, int highTime) {
        int bestHeight = getBestHeight();
        JSONArray received = ((bitmask & 5) != 0) ? 
                getReceivedByTime(address, lowTime, highTime, ((bitmask & 1) != 0), ((bitmask & 4) != 0), bestHeight) 
                : new JSONArray();
        JSONArray sent = ((bitmask & 2) != 0) ? getSentHistoryByTime(address, lowTime, highTime, bestHeight) : new JSONArray();
        return rearrange(received, sent).toString();
    }
    
    public JSONArray getBestBlocks(int n) {
        return selectAsJSON(
            "SELECT " +
                "height, " +
                "UNIX_TIMESTAMP() - time AS age, " +
                "hash, " +
                "address AS miner " +
            "FROM " + Database.BLOCKS + 
            " NATURAL JOIN " + Database.TXDETAILS + 
            " NATURAL JOIN " + Database.TRANSACTIONS +
            " NATURAL JOIN " + Database.OUTPUTS + 
            " NATURAL JOIN " + Database.ADDRESSES + 
                " WHERE height>(SELECT MAX(height)-" + n + 
                    " FROM " + Database.BLOCKS +
                ") AND vout=0" +
                " AND coinbase=true" +
            " ORDER BY height DESC; "
        );
    }
    
    
    
}
    
