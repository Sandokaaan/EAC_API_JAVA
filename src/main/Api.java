/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import database.Database;
import database.DbManager;
import java.sql.SQLException;
import java.util.Scanner;
import network.ApiListen;
import network.SyncManager;
import org.json.JSONArray;
import system.Config;
import system.Utils;

/**
 *
 * @author virtu
 */
public class Api {
   
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        Scanner sc = new Scanner(System.in, Utils.CHARSETNAME);
        
        DbManager dbm;
        try {
            dbm = new DbManager();
        } catch (SQLException ex) {
            System.err.println("Database initialization failed - " + ex.getMessage());
            return;
        }
        SyncManager sync = new SyncManager();
        ApiListen apiServer = new ApiListen();
        apiServer.start();
        sync.start();
        
        while (true) {
            String s = sc.nextLine();
            if (s.length() == 0) {
                System.out.println("This is log-console of JAVA-API. Write 'exit' to safely close database and exit API");
                continue;
            }
            if (s.equals("exit"))
                break;
            if (s.equals("stat"))
                System.out.println(dbm.getSyncStat().toString());
            else if (s.startsWith("addr")) {
                String[] slices = s.split(" ");
                if (slices.length == 2) {
                    JSONArray received = dbm.getReceivedHistory(slices[1], 10);
                    JSONArray send = dbm.getSentHistory(slices[1], 10);
                    System.out.println(dbm.rearrange(received, send));
                }
                else
                    System.out.println("invalid address");
            }
            else 
                System.out.println(dbm.selectAsJSON(s));
        }
        
        sync.close();
        try {
            sync.join();
        } catch (InterruptedException ex) {
            System.err.println(ex.getMessage());
        }        

        apiServer.close();
        try {
            apiServer.join();
        } catch (InterruptedException ex) {
            System.err.println(ex.getMessage());
        }        

        Database.closeAllConnections(Config.getConfig().dbName);

    }
        
}
