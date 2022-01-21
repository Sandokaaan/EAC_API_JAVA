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
public class Block {
    
    private final int height;
    private final String hash;
    private final int time;
    
    public Block(int height, String hash, int time) {
        this.height = height;
        this.hash = hash;
        this.time = time;
    }
    
    public String getSqlCommands() {
        return String.format(
            "REPLACE INTO %s (height, hash, time) VALUES(%d, '%s', %d); ",
            Database.BLOCKS, height, hash, time, hash, time
        );
    }
    
}
