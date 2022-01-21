/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package database.records;

import database.Database;
import java.util.LinkedList;
import java.util.Locale;

/**
 *
 * @author virtu
 */
public class Outputs {
    
    public class Output {
        private final String address;
        private final String txid;
        private final int vout;
        private final double value;

        public Output(String address, String txid, int vout, double value) {
            this.address = address;
            this.txid = txid;
            this.vout = vout;
            this.value = value;
        }
        
        public String getValue() {
            return String.format(Locale.US, 
                "((SELECT tx_id FROM %s WHERE txid='%s' LIMIT 1),%d,(SELECT addr_id FROM %s WHERE address='%s' LIMIT 1),%f)",
                Database.TRANSACTIONS, txid, vout, Database.ADDRESSES, address, value);
        }
        
    }

    private final LinkedList<Output> outputs;

    public Outputs() {
        this.outputs = new LinkedList<>();
    }
    
    public void add(String address, String txid, int vout, double value) {
        outputs.add(new Output(address, txid, vout, value));
    }
    
    public boolean isEmpty() {
        return outputs.isEmpty();
    }
    
    public String getSqlCommands() {
        LinkedList<String> values = new LinkedList<>();
        outputs.forEach(output -> {
            values.add(output.getValue());
        });
        String allValues = String.join(",", values);
        return String.format(Locale.US,
            "INSERT IGNORE INTO %s (tx_id, vout, addr_id, value) VALUES %s; ",
            Database.OUTPUTS, allValues
        );        
    }
    
}

