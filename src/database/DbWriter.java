/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database;

import database.records.Addresses;
import database.records.Block;
import database.records.Outputs;
import database.records.Spents;
import database.records.Transactions;
import database.records.TxDetails;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Deque;
import java.util.LinkedList;
import java.util.stream.Stream;
import system.Config;
import system.Task;

/**
 *
 * @author virtu
 */

public class DbWriter extends Task{

    public class DbBlockBuffer {
    
        public Block block;
        public final Transactions transactions;
        public final Addresses addresses;
        public final TxDetails txDetails;
        public final Spents spents;
        public final Outputs outputs;


        public DbBlockBuffer() {
            this.block = null;
            this.transactions = new Transactions();
            this.addresses = new Addresses();
            this.txDetails = new TxDetails();
            this.spents = new Spents();
            this.outputs = new Outputs();
        }
        
        public void setBlock(int height, String hash, int time) {
            this.block = new Block(height, hash, time);
        }

        public void addOutput(String address, String txid, int vout, double value) {
            addresses.add(address);
            outputs.add(address, txid, vout, value);
        }
    
        public void addSpent(String txid, int vout, String spending_txid) {
            transactions.add(txid);
            spents.add(txid, vout, spending_txid);
        }
    
        public void addTxDetail(String txid, int height, boolean coinbase, String txcomment, String ipfs) {
            transactions.add(txid);
            txDetails.add(txid, height, coinbase, txcomment, ipfs);
        }
    
    }
    
    private final Connection connection;
    private final Statement stmt;
    private final Deque<DbBlockBuffer> queue;
    private boolean exiting;


    public DbWriter() throws SQLException {
        super("DB_BUFFER");
        Config config = Config.getConfig();
        connection = DriverManager.getConnection(config.dbPrefix + config.dbName, config.dbUser, config.dbPassword);
        stmt = connection.createStatement();
        queue = new LinkedList<>();
        exiting = false;
    }
    
    @Override
    protected void initialization() {
        // nothing to do
    }

    @Override
    protected void mainTask() {
        while (!exiting) {
            synchronized(queue) {
                flush();
                try {
                    queue.wait();
                } catch (InterruptedException ex) {
                    exiting = true;
                }
            }
        }        
    }

    @Override
    protected void finished() {
        close();
    }
        
    public DbBlockBuffer getDbBlockBuffer() throws InterruptedException {
        return new DbBlockBuffer();
    }
    
    public void write(DbBlockBuffer b) {
        synchronized(queue) {
            while (queue.size() > 1)
                try {
                    queue.wait();
                } catch (InterruptedException ex) {
                    exiting = true;                    
                }
            queue.add(b);
            queue.notify();
        }
    }
        
    @SuppressWarnings("UseSpecificCatch")
    public void close() {
        exiting = true;
        try {
            stmt.close();
            connection.close();
        } catch (Exception ex) {}
        synchronized(queue) {
            queue.notify();
        }
    }
    
    
    private void flush() {
        DbBlockBuffer b;
        while (true) {
            synchronized(queue) {
                if (queue.isEmpty()) {
                    queue.notify();
                    return;
                }
                else
                    b = queue.peekFirst();
                queue.notify();
            }
            try {
                stmt.clearBatch();
                stmt.addBatch(b.block.getSqlCommands());
                if (!b.addresses.isEmpty()) 
                    stmt.addBatch(b.addresses.getSqlCommands());
                if (!b.transactions.isEmpty()) 
                    stmt.addBatch(b.transactions.getSqlCommands());
                if (!b.txDetails.isEmpty())
                    stmt.addBatch(b.txDetails.getSqlCommands());
                if (!b.outputs.isEmpty())
                    stmt.addBatch(b.outputs.getSqlCommands());
                if (!b.spents.isEmpty())
                    stmt.addBatch(b.spents.getSqlCommands());
                stmt.executeBatch();
            } catch (SQLException ex) {
                System.err.println(ex.getMessage());
            }
            synchronized(queue) {
                queue.pollFirst();  
                queue.notify();
            }
        }
    }
    
    private boolean checkTables() {
        try (ResultSet rs = stmt.executeQuery("Show tables");) {
            LinkedList<String> tablesInDatabase = new LinkedList<>();
            while(rs.next())
                tablesInDatabase.add(rs.getString(1).toLowerCase());
            LinkedList<String> tablesRequired = new LinkedList<>();
            Stream.of(Database.BLOCKS, Database.BLOCKS, 
                    Database.ADDRESSES, Database.TXDETAILS, 
                    Database.OUTPUTS,Database.SPENT)
                    .forEach((String x) -> tablesRequired.add(x.toLowerCase()));
            return tablesInDatabase.containsAll(tablesRequired);
        } catch (SQLException ex) {
            exiting = true;
            return false;
        }
    }
    
    private void createTable(String tableName, String...items) throws SQLException {
        String params = String.join(", ", items);
        String sql = String.format("CREATE TABLE %s (%s); ", tableName, params);
        stmt.executeUpdate(sql);
    }
    
    private void createTables() throws SQLException {
        System.out.print("Creating database tables ...");
        createTable(Database.BLOCKS,
            "height INTEGER NOT NULL PRIMARY KEY",
            "hash CHAR(64) NOT NULL",               
            "time INTEGER NOT NULL");
        createTable(Database.TRANSACTIONS,
            "tx_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY",
            "txid CHAR(64) UNIQUE NOT NULL");
        createTable(Database.TXDETAILS,
            "tx_id BIGINT NOT NULL PRIMARY KEY",    
            "height INTEGER NOT NULL",
            "coinbase BOOLEAN NOT NULL",
            "txcomment VARCHAR(252)",
            "ipfs VARCHAR(90)");
         createTable(Database.ADDRESSES,
            "addr_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY",
            "address VARCHAR(90) UNIQUE NOT NULL");
         createTable(Database.OUTPUTS, 
            "tx_id BIGINT NOT NULL",
            "vout SMALLINT NOT NULL",
            "addr_id BIGINT NOT NULL",
            "value DOUBLE NOT NULL",
            "PRIMARY KEY (vout, tx_id)");
        createTable(Database.SPENT, 
            "tx_id BIGINT NOT NULL",
            "vout SMALLINT NOT NULL",
            "spending_tx_id BIGINT NOT NULL",
            "PRIMARY KEY (vout, tx_id)");
        System.out.println("Database ready");        
    }
    
    private void deleteTable(String tableName) {
        try {
            String sql = String.format("DROP TABLE %s; ", tableName);
            stmt.executeUpdate(sql);
        } catch (SQLException ex) {
        }
    }
    
    private void deleteTables() throws SQLException {
        deleteTable(Database.BLOCKS);
        deleteTable(Database.TRANSACTIONS);
        deleteTable(Database.ADDRESSES);
        deleteTable(Database.TXDETAILS);
        deleteTable(Database.OUTPUTS);
        deleteTable(Database.SPENT);
    }
    
    private void addIndex(String table, String collumn) throws SQLException {
        String sql = String.format("ALTER TABLE %s ADD INDEX (%s); ", table, collumn);
        stmt.executeUpdate(sql);
    }
    
    public void createIndexes() {
        try {
            System.out.println("Indexing tables...");
            addIndex(Database.BLOCKS, "time");
            addIndex(Database.BLOCKS, "hash");
            addIndex(Database.TXDETAILS, "height");
            addIndex(Database.OUTPUTS, "addr_id");
            addIndex(Database.OUTPUTS, "tx_id");
            addIndex(Database.SPENT, "spending_tx_id");
            addIndex(Database.SPENT, "tx_id");
            System.out.println("Indexing finished.");
        } catch (SQLException ex) {
            System.err.println("Indexing failed - " + ex.getMessage());
        }
    } 
    
    public void checkOrCreateTables() {
        System.out.println("Check tables...");
        try {
            if (!checkTables()) {
                System.out.println("Preparing tables...");
                deleteTables();
                createTables();
                createIndexes();
                System.out.println("Tables created.");
            } 
            System.out.println("Database is ready.");
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
            exiting = true;
        }
    }
    
}


