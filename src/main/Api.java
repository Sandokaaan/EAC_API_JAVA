/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import network.ApiListen;
import network.SyncManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileReader;

/**
 *
 * @author virtu
 */
public class Api {
   
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        SyncManager sync = new SyncManager();
        ApiListen apiServer = new ApiListen();
        apiServer.start();
        sync.start();
        
        BufferedReader in = null;
        try{
          in = new BufferedReader(new FileReader("/tmp/apififo"));
          while(in.ready()){
             String s = in.readLine();
             System.err.println("FIFO signal received -> exiting...");
          }
          in.close();
        }catch(IOException ex){
          System.err.println("FIFO signal failed -> exiting...");
        }
       
        apiServer.close();
        sync.close();

    }
        
}

