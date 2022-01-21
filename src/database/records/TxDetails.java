/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.records;

import database.Database;
import java.util.LinkedList;

/**
 *
 * @author virtu
 */
public class TxDetails {
    
    public class TxDetail {
        public final String txid;
        public final int height;
        public final boolean coinbase;
        public final String txcomment;
        public final String ipfs;

        public TxDetail(String txid, int height, boolean coinbase, String txcomment, String ipfs) {
            this.txid = txid;
            this.height = height;
            this.coinbase = coinbase;
            this.txcomment = (txcomment!=null) ? txcomment.replaceAll("[^a-zA-Z0-9]", " ") : txcomment;
            this.ipfs = ipfs;
        }
        
        public String getValue() {
            return String.format("((SELECT tx_id FROM %s WHERE txid='%s' LIMIT 1),%d,'%s','%s',%s)",
                Database.TRANSACTIONS, txid, height, txcomment, ipfs, (coinbase?"TRUE":"FALSE"));
        }
    }
        
            
    private final LinkedList<TxDetail> txDetails; 

    public TxDetails() {
        txDetails = new LinkedList<>();
    }
    
    public void add(String txid, int height, boolean coinbase, String txcomment, String ipfs) {
        txDetails.add(new TxDetail(txid, height, coinbase, txcomment, ipfs));
    }
    
    public boolean isEmpty() {
        return txDetails.isEmpty();
    }

    public String getSqlCommands() {
        LinkedList<String> values = new LinkedList<>();
        txDetails.forEach(txDetail -> {
            values.add(txDetail.getValue());
        });
        String allValues = String.join(",", values);
        return String.format(
            "REPLACE INTO %s (tx_id, height, txcomment, ipfs, coinbase) VALUES %s; ",
            Database.TXDETAILS, allValues
        );
    }
    
}
       
