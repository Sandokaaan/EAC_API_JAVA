/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import database.Database;
import database.DbReader;
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
        }
        
        apiServer.close();
        sync.close();

    }
        
}
