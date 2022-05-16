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
public class DbManager {
    
    private static final String BLOCKS = "blockchain";
    private static final String TRANSACTIONS = "transactions";
    private static final String TXDETAILS = "txdetails";
    private static final String OUTPUTS = "outputs";
    private static final String SPENT = "spent";
    private static final String ADDRESSES = "addresses";
    private Database db = null; 
    private boolean connected = false;
    
    /*
    * Constructor - open a new connection to the database
    */    
    public DbManager() throws SQLException {
        db = new Database();
        createTables();         // create only if not exist
        connected = true;
    }
    
    /*
    * Close the connection to the database
    * The creator JSONArrayOf DbManager object must call close on finishing
    */
    public final void close() {
        db.close();
        connected = false;
    }
    
    /*
    * Create tables only if their do not exists
    */
    private void createTables() throws SQLException {
        System.out.print("Check/create tables ...");
        db.createTable(BLOCKS,
                "height INTEGER NOT NULL PRIMARY KEY",
                "hash CHAR(64) UNIQUE NOT NULL",
                "time INTEGER NOT NULL");
        db.createTable(TRANSACTIONS,
                "tx_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY",
                "txid CHAR(64) UNIQUE NOT NULL");
        db.createTable(ADDRESSES,
                "addr_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY",
                "address VARCHAR(90) NOT NULL");
        db.createTable(TXDETAILS,
                "tx_id BIGINT NOT NULL PRIMARY KEY",                
                "height INTEGER NOT NULL",
                "coinbase BOOLEAN",
                "ipfs VARCHAR(90)",
                "txcomment VARCHAR(252)");
        db.createTable(OUTPUTS, 
                "tx_id BIGINT NOT NULL",
                "vout SMALLINT NOT NULL",
                "addr_id BIGINT NOT NULL",
                "value DOUBLE",
                "PRIMARY KEY (tx_id, vout)");
        db.createTable(SPENT, 
                "tx_id BIGINT NOT NULL",
                "vout SMALLINT NOT NULL",
                "spending_tx_id BIGINT NOT NULL",
                "PRIMARY KEY (tx_id, vout)");
        db.command(String.format(
                "CREATE INDEX IF NOT EXISTS ihash ON %s(hash)"
                , BLOCKS));
        db.command(String.format(
                "CREATE INDEX IF NOT EXISTS itxid ON %s(txid)"
                , TRANSACTIONS));
        db.command(String.format(
                "CREATE INDEX IF NOT EXISTS iaddr ON %s(address)"
                , ADDRESSES));
        db.command(String.format(
                "CREATE INDEX IF NOT EXISTS iheight ON %s(height)"
                , TXDETAILS));
        db.command(String.format(
                "CREATE INDEX IF NOT EXISTS iaddro ON %s(addr_id)"
                , OUTPUTS));
        db.command(String.format(
                "CREATE INDEX IF NOT EXISTS itxido ON %s(tx_id)"
                , OUTPUTS));
        db.command(String.format(
                "CREATE INDEX IF NOT EXISTS istxids ON %s(tx_id)"
                , SPENT));
        db.command(String.format(
                "CREATE INDEX IF NOT EXISTS itxids ON %s(spending_tx_id)"
                , SPENT));
        System.out.println(" OK");
    } 


/*
* Section - methods for blocks
*/
    
    public int getBlockCount() {
        return db.count(BLOCKS);
    }
    
    public int addBlock(int height, String hash, int time) {
        return db.commandWithParams(
            "SET @PARAMS = (?,?,?); " +
            "MERGE INTO " + BLOCKS + 
                " (height, hash, time) KEY(height) " + 
                "VALUES (@PARAMS[0], @PARAMS[1], @PARAMS[2]); ",
            params(height, hash, time)    
        );
    }
    
    public JSONObject getBlock(String hash) {
        return selectFirstAsJSON(
            "SELECT * FROM %s WHERE hash='%s' LIMIT 1; ", 
            BLOCKS, hash
        );
    }    
    
    public JSONObject getBlock(int height) {
        return selectFirstAsJSON(
            "SELECT * FROM %s WHERE height=%d LIMIT 1; ", 
            BLOCKS, height
        );
    }
    
    public JSONObject getBestBlock() {
        return selectFirstAsJSON(
            "SELECT hash FROM %s WHERE height=(SELECT MAX(height) FROM %1$s) LIMIT 1; ", 
            BLOCKS
        );        
    }

    public int getBestHeight() {
        return selectFirstAsJSON(
            "SELECT MAX(height) AS height FROM %s; ", 
            BLOCKS
        ).optInt("height", -1);          
    }

    public int getLowestHeight() {
        return selectFirstAsJSON(
            "SELECT MIN(height) AS height FROM %s; ", 
            BLOCKS
        ).optInt("height", -1);          
    }
    
    public String getBestHash() {
        return getBestBlock().optString("hash", "");
    }
    
    public int getHighestTime() {
        return getBestBlock().optInt("time", Integer.MAX_VALUE);
    }    

    public int getLowestTime() {
        return getBlock(0).optInt("time", Integer.MIN_VALUE);
    }    
    
    public String getBlockHash(int height) {
        return getBlock(height).optString("hash", "");
    }
    
    public int getBlockHeight(String hash) {
        return getBlock(hash).optInt("height", -1);
    }
    
    public int getBlockTime(int height) {
        return getBlock(height).optInt("time", -1);
    }    
    
    public TreeSet<Object> getAllHeights() {
        return db.selectSet(String.format(
            "SELECT height FROM %s ;",
            BLOCKS
        ));
    }

/*
* Section - methods for transactions
*/
    
    public int addTransaction(String txid) {
        return db.commandWithParams(
            "SET @PARAMS = (?); " +
            "MERGE INTO " + TRANSACTIONS + 
                " (txid) KEY(txid) VALUES (@PARAMS[0]); ",
            params(txid)    
        );
    }
    
    public int addTransactionWithDetails(String txid, int height, String ipfs, String txcomment, boolean coinbase) {
        return db.commandWithParams(
            "SET @PARAMS = (?,?,?,?); " +
            "MERGE INTO " + TRANSACTIONS + " (txid) KEY(txid) VALUES (@PARAMS[0]); " +        
            "SET @TX_ID = (SELECT tx_id FROM " + 
                            TRANSACTIONS + 
                            " WHERE txid='" + txid + 
                            "' LIMIT 1); " + 
            "MERGE INTO " + TXDETAILS + 
                " (tx_id, height, ipfs, txcomment, coinbase) KEY(tx_id) " +
                " VALUES (@TX_ID, @PARAMS[1], @PARAMS[2], @PARAMS[3], " + 
                (coinbase?"TRUE":"NULL") + "); ",
            params(txid, height, ipfs, txcomment)    
        );
    }    
    
    public int getTransactionCount() {
        return db.count(TRANSACTIONS);
    }
    
    public TreeSet<Object> getMostFreshHeights() {
        return db.selectSet(String.format(
            "SELECT height FROM %s WHERE tx_id>"
                + "(SELECT MAX(tx_id)-500 FROM %1$s );",
            TXDETAILS
        ));        
    }
//select height from txdetails where tx_id>(select MAX(tx_id)-100 from txdetails);
    
/*
* Section - methods for transaction-outputs
*/
    
    public int addTxOut(String txid, int vout, String address, double value) {
        return db.commandWithParams(
            "SET @PARAMS = (?,?,?); " +
            "MERGE INTO " + ADDRESSES + " (address) KEY(address) VALUES (@PARAMS[1]); " +        
            "SET @ADDR_ID = (SELECT addr_id FROM " + ADDRESSES + 
                " WHERE address='" + address + "' LIMIT 1); " +                     
            "SET @TX_ID = (SELECT tx_id FROM " + TRANSACTIONS + 
                " WHERE txid='" + txid + "' LIMIT 1); " +         
            "MERGE INTO " + OUTPUTS + 
                " (tx_id,vout,addr_id,value) KEY(tx_id,vout) " +
                "VALUES (@TX_ID, @PARAMS[0], @ADDR_ID, @PARAMS[2]); ",
            params(vout, address, value)    
        );
    }    

/*
* Section - methods for transaction-spent
*/
    
    public int addTxSpent(String src_txid, int src_vout, String spending_txid){
        return db.commandWithParams(
            "SET @PARAMS = (?,?); " +
            "MERGE INTO " + TRANSACTIONS + 
                " (txid) KEY(txid) VALUES (@PARAMS[0]); " +                    
            "SET @SRC_TX_ID = (SELECT tx_id FROM " + TRANSACTIONS + 
                                    " WHERE txid='" + src_txid + 
                                    "' LIMIT 1); " +  
            "SET @SPENDING_TX_ID = (SELECT tx_id FROM " + TRANSACTIONS +
                                    " WHERE txid='" + spending_txid + 
                                    "' LIMIT 1); " +   
            "MERGE INTO " + SPENT + 
                " (tx_id, vout, spending_tx_id) KEY(tx_id,vout) " +
                "VALUES (@SRC_TX_ID, @PARAMS[1], @SPENDING_TX_ID); ",
            params(src_txid, src_vout)    
        );        
    }
    
/*
* Section JSONArrayOf private supporting methods    
*/
    
    private JSONArray selectAsJSON(String query, Object...objects){
        return db.select((String.format(query, objects)));
    }
    
    public JSONArray selectAsJSON(String query){
        //System.err.println(query);
        return db.select(query);
    }
    
    private JSONObject selectFirstAsJSON(String query, Object...objects){
        return db.selectOne(String.format(query, objects));
    }

    private Object[] objects(Object...objects){
        return objects;
    }
    
    private Object[] params(Object...params){
        return params;
    }
    
    /*
    * Section complex queries
    */
    
    public JSONObject getSyncStat() {
        return selectFirstAsJSON(
            "SELECT "
                + "CONVERT( 100*COUNT(*)/(1.0+MAX(height)), REAL ) AS sync, " 
                + "COUNT(*) AS count, "
                + "MAX(height) AS height "
            + "FROM " + BLOCKS + ";"
        );
    }
    
    public JSONArray getSentHistory(String address, int limit) {
        //System.out.println("sent");
        return selectAsJSON(
            "SELECT " +
                "'sent' AS type, " +
                "t.txid, " +
                "b.hash, " +
                "d.height, " +    
                "b.time,"  +   
                "d.txcomment, " +
                "d.ipfs, " +
                "o2.vout, " +
                "a2.address, " + 
                "o2.value " +    
            "FROM (" +
                "SELECT * FROM " + TRANSACTIONS + " " +
                    "WHERE tx_id IN (" +
                        "SELECT s.spending_tx_id AS tx_id " +
                        "FROM (" +
                            "SELECT * FROM " + ADDRESSES + " " +
                                "WHERE address='" + address +
                            "') AS a " +
                        "LEFT JOIN " + OUTPUTS + " AS o " +
                            "ON a.addr_id=o.addr_id " +
                        "LEFT JOIN " + SPENT + " AS s " +
                            "ON o.tx_id=s.tx_id AND o.vout=s.vout " +
                    ")" +
            ") AS t " +
            "LEFT JOIN " + TXDETAILS + " AS d " +
                "ON t.tx_id=d.tx_id " +
            "LEFT JOIN " + BLOCKS + " AS b " +
                "ON d.height=b.height " +
            "LEFT JOIN " + OUTPUTS + " AS o2 " +
                "ON t.tx_id=o2.tx_id " +
            "LEFT JOIN " + ADDRESSES + " AS a2 " +
                "ON o2.addr_id=a2.addr_id " +
            "ORDER BY " +
                "d.height DESC, t.txid, o2.vout ASC " +                        
            "LIMIT " + limit        

        );
    }
    
    public JSONArray getReceivedHistory(String address, int limit) {
        //System.out.println("received");
        int bestHeight = getBestHeight();
        return selectAsJSON(
            "SELECT " +
                "CASE WHEN coinbase=true THEN 'mined' ELSE 'received' END AS type, " +
                "a.address, " +
                "(SELECT address FROM " + SPENT + " AS s " +
                    "LEFT JOIN " + OUTPUTS + " AS o " +
                        "ON o.tx_id=s.tx_id AND o.vout=s.vout " +
                    "LEFT JOIN " + ADDRESSES + " AS a " +
                        "ON a.addr_id=o.addr_id " +
                    "WHERE s.spending_tx_id=t.tx_id " +
                    "LIMIT 1 " +
                ") AS sender, " +                    
                "o.value, " +
                "CASE WHEN spending_tx_id IS NULL THEN false ELSE true END AS spent," +    
                "t.txid, " +
                "o.vout, " +
                "b.time, " +
                "d.height, " +
                "(" + bestHeight + " - d.height)  AS confirmations, " +
                "b.hash, " +
                "d.txcomment, " +
                "d.ipfs " +    
            "FROM (" +
                "SELECT * " +
                "FROM " + ADDRESSES + " " +
                    "WHERE address='" + address +
            "') AS a " +
            "LEFT JOIN " + OUTPUTS + " AS o " +
                "ON a.addr_id=o.addr_id " +
            "LEFT JOIN " + TXDETAILS + " AS d " +
                "ON o.tx_id=d.tx_id " +
            "LEFT JOIN " + SPENT + " AS s " +
                "ON o.tx_id=s.tx_id AND o.vout=s.vout " +
            "LEFT JOIN " + TRANSACTIONS + " AS t " +
                "ON o.tx_id=t.tx_id " +
            "LEFT JOIN " + BLOCKS + " AS b " +
                "ON d.height=b.height " +
            "ORDER BY " +
                "d.height DESC, t.txid, o.vout ASC " +        
            "LIMIT " + limit + "; "
        );
    }
    
    public JSONArray rearrange(JSONArray received, JSONArray sent){
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
                        .of("type", "txid", "time", "height", "hash", "txcomment", "ipfs")
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
    
    public JSONObject addressStatReceived(String address) {
        return selectFirstAsJSON(
            "SELECT " +
                "COUNT(coinbase) AS mined, " +
                "COUNT(o.tx_id) AS inputs, " +
                "SUM(value) AS total_value, " +
                "SUM(CASE WHEN coinbase=true THEN 0 ELSE value END) AS received_value, " +
                "MIN(d.height) AS low_block, " +
                "MAX(d.height) AS high_block, " +
                "COUNT(ipfs) AS with_ipfs, " +
                "COUNT(txcomment) AS with_txcomment, " +
                "MIN(time) AS low_time, " +     
                "MAX(time) AS high_time " +
            "FROM (SELECT '" +
                address + "' AS address) AS param " +
            "NATURAL JOIN " + ADDRESSES + " AS a " +
            "NATURAL JOIN " + OUTPUTS + " AS o " +        
            "NATURAL JOIN " + TXDETAILS + " AS d " +
            "NATURAL JOIN " + BLOCKS + " AS b; "        
        );
    }

    public JSONObject addressStatSent(String address) {
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
            "NATURAL JOIN " + ADDRESSES + " AS a " +
            "NATURAL JOIN " + OUTPUTS + " AS o " +   
            "NATURAL JOIN " + SPENT + " AS s " +        
            "INNER JOIN " + TXDETAILS + " AS d " +
                "ON s.spending_tx_id=d.tx_id " +
            "NATURAL JOIN " + BLOCKS + " AS b; "        
        );
    }    
    
    public JSONObject addressStatistics(String address) {
        JSONObject sentStat = addressStatSent(address);
        JSONObject recStat = addressStatReceived(address);
        JSONObject rtsJson = new JSONObject();
        setJsonOrdered(rtsJson);
        try {
            int nInputs = recStat.optInt("inputs", 0);
            int nMined = recStat.optInt("mined", 0);
            int nReceived = nInputs - nMined;
            double totalInputValue = recStat.optDouble("total_value", 0);
            double receivedValue = recStat.optDouble("received_value", 0);
            double minedValue = totalInputValue - receivedValue;
            int lowBlockReceived = recStat.optInt("low_block", 0);
            int highBlockReceived = recStat.optInt("high_block", 0);
            int lowBlockSent = sentStat.optInt("low_block", 0);
            int highBlockSent = sentStat.optInt("high_block", 0);
            int lowTimeReceived = recStat.optInt("low_time", 0);
            int highTimeReceived = recStat.optInt("high_time", 0);
            int lowTimeSent = sentStat.optInt("low_time", 0);
            int highTimeSent = sentStat.optInt("high_time", 0);
            int nReceivedWithIpfs = recStat.optInt("with_ipfs", 0);
            int nReceivedWithComment = recStat.optInt("with_txcomment", 0);
            int nOutputs = sentStat.optInt("outputs", 0);
            int nSent = sentStat.optInt("transactions", 0);
            double sentValue = sentStat.optDouble("total_value", 0);
            int nSentWithIpfs = sentStat.optInt("with_ipfs", 0);
            int nSentWithComment = sentStat.optInt("with_txcomment", 0);
            double balance = totalInputValue - sentValue;
            // results shown
            rtsJson.put("address", address);
            rtsJson.put("balance", balance);
            rtsJson.put("transaction_count", nReceived + nSent);
            rtsJson.put("received", nReceived);
            rtsJson.put("sent", nSent);
            rtsJson.put("mined", nMined);
            rtsJson.put("unspent_outputs", nInputs - nOutputs);
            rtsJson.put("received_value", receivedValue);
            rtsJson.put("sent_value", sentValue);
            rtsJson.put("mined_value", minedValue);
            rtsJson.put("low_block", Math.min(lowBlockSent, lowBlockReceived));
            rtsJson.put("high_block", Math.max(highBlockSent, highBlockReceived));
            rtsJson.put("low_time", Math.min(lowTimeSent, lowTimeReceived));
            rtsJson.put("high_time", Math.max(highTimeSent, highTimeReceived));
            rtsJson.put("received_comments", nReceivedWithComment);
            rtsJson.put("received_ipfs", nReceivedWithIpfs);
            rtsJson.put("sent_comments", nSentWithComment);
            rtsJson.put("sent_ipfs", nSentWithIpfs);
        } catch (JSONException ex) {
            System.err.println("Error - addressStatistics " + ex.getMessage());
        }
        return rtsJson;
    }
    
    private JSONArray getReceivedByTime(String address, int lowTime, int highTime, boolean received, boolean mined) {
        boolean both = received && mined;
        int bestHeight = getBestHeight();
        return selectAsJSON("SELECT " +
                "CASE WHEN coinbase=true THEN 'mined' ELSE 'received' END AS type, " +
                "a.address, " +
                "(SELECT address FROM " + SPENT + " AS s " +
                    "LEFT JOIN " + OUTPUTS + " AS o " +
                        "ON o.tx_id=s.tx_id AND o.vout=s.vout " +
                    "LEFT JOIN " + ADDRESSES + " AS a " +
                        "ON a.addr_id=o.addr_id " +
                    "WHERE s.spending_tx_id=t.tx_id " +
                    "LIMIT 1 " +
                ") AS sender, " +                  
                "o.value, " +
                "CASE WHEN spending_tx_id IS NULL THEN false ELSE true END AS spent," +    
                "t.txid, " +
                "o.vout, " +
                "b.time, " +
                "d.height, " +
                "(" + bestHeight + " - d.height)  AS confirmations, " +                            
                "b.hash, " +
                "d.txcomment, " +
                "d.ipfs " + 
            "FROM (SELECT '" + address + "' AS address) AS param " +
            "NATURAL JOIN " + ADDRESSES + " AS a " +
            "LEFT JOIN " + OUTPUTS + " AS o " +
                "ON o.addr_id=a.addr_id " +   
            "LEFT JOIN " + TXDETAILS + " AS d " +
                "ON o.tx_id=d.tx_id " +
            "LEFT JOIN " + TRANSACTIONS + " AS t " +
                "ON o.tx_id=t.tx_id " +        
            "LEFT JOIN " + BLOCKS + " AS b " +
                "ON d.height=b.height " +        
            "LEFT JOIN " + SPENT + " AS s " +
                "ON o.tx_id=s.tx_id AND o.vout=s.vout " +
            "WHERE b.time BETWEEN " + lowTime + " AND " + highTime +
                ((both) ? "" : " AND coinbase IS " + (mined ? "NOT ": " ") + "NULL") +
            " ORDER BY " +
                "d.height DESC, t.txid, o.vout ASC ; "
        );
    }

    public JSONArray getSentHistoryByTime(String address, int lowTime, int highTime) {
        //System.out.println("sent");
        return selectAsJSON("SELECT " +
                "'sent' AS type, " +
                "t.txid, " +
                "b.hash, " +
                "d.height, " +    
                "b.time,"  +   
                "d.txcomment, " +
                "d.ipfs, " +
                "o2.vout, " +
                "a2.address, " + 
                "o2.value " +    
            "FROM (" +
                "SELECT * FROM " + TRANSACTIONS + " " +
                    "WHERE tx_id IN (" +
                        "SELECT s.spending_tx_id AS tx_id " +
                        "FROM (" +
                            "SELECT * FROM " + ADDRESSES + " " +
                                "WHERE address='" + address +
                            "') AS a " +
                        "LEFT JOIN " + OUTPUTS + " AS o " +
                            "ON a.addr_id=o.addr_id " +
                        "LEFT JOIN " + SPENT + " AS s " +
                            "ON o.tx_id=s.tx_id AND o.vout=s.vout " +
                        "LEFT JOIN " + TXDETAILS + " AS d " +     
                            "ON d.tx_id=s.spending_tx_id " + 
                        "LEFT JOIN " + BLOCKS + " AS b " +     
                            "ON b.height=d.height " + 
                            "WHERE b.time between " + lowTime + " AND " + highTime +
                    ")" +
            ") AS t " +
            "LEFT JOIN " + TXDETAILS + " AS d " +
                "ON t.tx_id=d.tx_id " +
            "LEFT JOIN " + BLOCKS + " AS b " +
                "ON d.height=b.height " +
            "LEFT JOIN " + OUTPUTS + " AS o2 " +
                "ON t.tx_id=o2.tx_id " +
            "LEFT JOIN " + ADDRESSES + " AS a2 " +
                "ON o2.addr_id=a2.addr_id " +
            "ORDER BY " +
                "d.height DESC, t.txid, o2.vout ASC "
        );
    }
    
    
    public String getHistorryByHeight(String address, int bitmask, int lowBlock, int highBlock) {
        int lowTime = getBlockTime(lowBlock);
        int highTime = getBlockTime(highBlock);
        return getHistorryByTime(address, bitmask, lowTime, highTime);
    }

    public String getHistorryByTime(String address, int bitmask, int lowTime, int highTime) {
        JSONArray received = ((bitmask & 5) != 0) ? 
                getReceivedByTime(address, lowTime, highTime, ((bitmask & 1) != 0), ((bitmask & 4) != 0)) 
                : new JSONArray();
        JSONArray sent = ((bitmask & 2) != 0) ? getSentHistoryByTime(address, lowTime, highTime) : new JSONArray();
        return rearrange(received, sent).toString();
    }
    
}
    
