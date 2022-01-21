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
public class Spents {
    
    public class Spent {
    
        private final String txid;
        private final int vout;
        private final String spending_txid;

        public Spent(String txid, int vout, String spending_txid) {
            this.txid = txid;
            this.vout = vout;
            this.spending_txid = spending_txid;
        }
        
        public String getValue() {
            return String.format("((SELECT tx_id FROM %s WHERE txid='%s' LIMIT 1),%d,(SELECT tx_id FROM %s WHERE txid='%s' LIMIT 1))",
                Database.TRANSACTIONS, txid, vout, Database.TRANSACTIONS, spending_txid );
        }
        
    }
    
    private final LinkedList<Spent> spents;

    public Spents() {
        spents = new LinkedList<>();
    }
    
    public boolean isEmpty() {
        return spents.isEmpty();
    }
    
    public void add(String txid, int vout, String spending_txid) {
        
        spents.add(new Spent(txid, vout, spending_txid));
    }
    
    public String getSqlCommands() {
        LinkedList<String> values = new LinkedList<>();
        spents.forEach(spent -> {
            values.add(spent.getValue());
        });
        String allValues = String.join(",", values);
        return String.format(
            "REPLACE INTO %s (tx_id, vout, spending_tx_id) VALUES %s; ",
            Database.SPENT, allValues
        );
    }
    
}
