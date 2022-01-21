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
public class Transactions {
    
    private final LinkedList<String> transactions;
    
    public Transactions() {
        transactions = new LinkedList<>();
    }
    
    public void add(String txid) {
        if (!transactions.contains(txid))
            transactions.add(txid);
    }
    
    public boolean isEmpty() {
        return transactions.isEmpty();
    }
    
    public String getSqlCommands() {
        String values = String.join("'),('", transactions);
        return String.format(
            "INSERT IGNORE INTO %s (txid) VALUES('%s'); ",
            Database.TRANSACTIONS, values
        );
    }
    
}
