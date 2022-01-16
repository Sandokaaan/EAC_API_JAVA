/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network;

import database.DbManager;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.PriorityQueue;
import java.util.TreeSet;
import java.util.stream.IntStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import system.Config;
import system.Task;

/**
 *
 * @author virtu
 */
public class SyncManager extends Task {
    private DbManager dbManager;
    private boolean exiting;
    private P2pClient p2pClient;
    private RpcClient rpcClient;
    private final PriorityQueue<Integer> blocks;
    private final Config config;
    // private boolean withoutDatabase; - ale do nějaké vyšší úrovně, zde se nedostane při true

    public SyncManager() {
        super("JAVA_SYNC_MANAGER");
        exiting = false;
        dbManager = null;
        p2pClient = null;
        rpcClient = null;
        blocks = new PriorityQueue<>(Collections.reverseOrder());    // poll returns the higher value in the Queue
        config = Config.getConfig();
    }

    @Override
    protected void initialization() {
        rpcClient = new RpcClient(config.rpcUrl, config.rpcPortNumber, config.rpcUserName, config.rpcPassword);
        try {
            dbManager = new DbManager();
        } catch (SQLException ex) {
            System.err.println("SyncManager failed to open a database manager: " + ex.getMessage());
            exiting = true;
            return;
        }       
        p2pClient = new P2pClient(this);
        p2pClient.start();
    }

    @Override
    protected void mainTask() {
        dispatchBlocks(Integer.MAX_VALUE);    // get best header just when started
        checkIntegrity();
        while (!exiting) {
            int blockIndex = -1;
            try {
                synchronized(blocks) {
                    if (!blocks.isEmpty())
                        blockIndex = blocks.poll();
                    else
                        blocks.wait();
                }
            } catch (InterruptedException ex) {
                exiting = true;
                System.err.printf(this.getName() + " was interrupted" );
            }
            if (blockIndex>-1)
                dispatchBlocks(blockIndex);
        }
    }

    @Override
    protected void finished() {
        exiting = true;
        if (p2pClient != null)
            p2pClient.disconnect();
        p2pClient = null;
        if (dbManager != null)
            dbManager.close();
    }
    
    public void addBlockRequest(int blockIndex) {
         synchronized(blocks) {
             if (!blocks.contains(blockIndex))     // requests only onece
                blocks.add(blockIndex);
             blocks.notify();
         }         
    }
    
    private void dispatchBlocks(int blockIndex) {
        if (blockIndex > -1) {
            RpcRequest request = (blockIndex == Integer.MAX_VALUE) ? 
                    RpcRequest.of("getbestblockhash"):
                    RpcRequest.of("getblockhash", blockIndex);
            try {
                String hash = rpcClient.query(request).getAsString();
                request = RpcRequest.of("getblock", hash, 2);
                RpcResponse block = rpcClient.query(request);
                storeToDatabase(block);
            } catch (IOException ex) {
                System.err.println("dispatchBlocks() RPC request failed " + ex.getMessage());
            }
        }
    }
    
    public void close() {
        exiting = true;
        addBlockRequest(-1);
    }
    
    private void storeToDatabase(RpcResponse block) {
        try {
            JSONObject jsonBlock = block.getAsJson();
            int height = jsonBlock.getInt("height");
            int ntx = jsonBlock.getInt("nTx");
            String hash = jsonBlock.getString("hash");
            int time = jsonBlock.getInt("time");
            dbManager.addBlock(height, hash, time);
            String prevHash = jsonBlock.getString("previousblockhash");
            String dbPrevHash = dbManager.getBlockHash(height-1);
            if (!prevHash.equals(dbPrevHash) && height>0) {
                addBlockRequest(height-1);
            }
            analyzeTransactions(jsonBlock, height);
        } catch (JSONException ex) {
            System.err.println("storeToDatabase() failed " + ex.getMessage());
        }
    }
    
    private void analyzeTransactions(JSONObject block, int height){
        try {
            JSONArray transactions = block.getJSONArray("tx");
            for (int i=0; i<transactions.length(); i++) {
                JSONObject tx = transactions.getJSONObject(i);
                String txid = tx.getString("txid");
                String ipfs = tx.optString("IPFS_CID", null);
                String txcomment = tx.optString("txComment", null);
                dbManager.addTransactionWithDetails(txid, height, ipfs, txcomment, (i==0));
                JSONArray outputs = tx.getJSONArray("vout");
                JSONArray vin = tx.getJSONArray("vin");
                for (int j=0; j<vin.length(); j++) {
                    JSONObject input = vin.getJSONObject(j);
                    String src_txid = input.optString("txid", null);
                    int src_vout = input.optInt("vout", -1);  // exclude coinbase inputs
                    if (src_txid != null && src_vout>=0)
                        dbManager.addTxSpent(src_txid, src_vout, txid);
                }
                outputs.forEach(output -> {analyzeOutput((JSONObject) output, txid);});            
            }
        } catch (JSONException ex) {
            System.err.println("analyzeTransactions() failed " + ex);
        }
    }

    private void analyzeOutput(JSONObject output, String txid){
        try {
            int vout = output.getInt("n");
            String address = output.getJSONObject("scriptPubKey").getJSONArray("addresses").getString(0);
            double value = output.getDouble("value");
            dbManager.addTxOut(txid, vout, address, value);
        } catch (JSONException ex) {
            // ignore outputs without address
            //System.err.println("analyzeOutput() failed " + ex);
            //System.err.println(output.toString());
        }
    }
    
    private void checkIntegrity() {
        System.out.println("Check integrity...");
        int lowBlock = dbManager.getLowestHeight();
        int bestBlock = dbManager.getBestHeight();
        if (lowBlock>0) {
            System.out.println("checkIntegrity() - low block is " + lowBlock + " - requested ");
            addBlockRequest(lowBlock-1);
        }
        TreeSet<Object> mostRecentBlocks = dbManager.getMostFreshHeights();
        mostRecentBlocks.stream().forEach( o -> blocks.add((int)o));
        System.out.println("Deep check for blocks " + mostRecentBlocks.last() + " - " + mostRecentBlocks.first());
        System.out.println("checkIntegrity() - block range " + lowBlock + " - " + bestBlock);
        TreeSet<Object> blocksInDb = dbManager.getAllHeights();
        IntStream
            .rangeClosed(lowBlock, bestBlock)
            .filter( o -> !blocksInDb.contains(o) )
            .forEach( o -> blocks.add(o));
        System.out.println("checkIntegrity() - finished");
    }

}

