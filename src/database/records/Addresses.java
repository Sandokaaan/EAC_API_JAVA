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
public class Addresses {
    
    private final LinkedList<String> addresses;
    
    public Addresses() {
        addresses = new LinkedList<>();
    }
    
    public void add(String address) {
        if (!addresses.contains(address))
            addresses.add(address);
    }

    public boolean isEmpty() {
        return addresses.isEmpty();
    }
    
    public String getSqlCommands() {
        String values = String.join("'),('", addresses);
        return String.format(
            "INSERT IGNORE INTO %s (address) VALUES('%s'); ",
            Database.ADDRESSES, values
        );
    }
    
}
