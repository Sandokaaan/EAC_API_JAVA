/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package apiextension;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


/**
 *
 * @author Vratislav Bednařík
 */
public class Config {
    private static Config singleton = null;
    private String rpcUrl = "127.0.0.1";
    private int rpcPortNumber = 15678;
    private String rpcUserName = "username";
    private String rpcPassword = "password";
    private String apiUrl = "127.0.0.1";
    private int apiPortNumber = 9000;
    public final int rpcport;
    public final int apiport;
    public final String rpcaddr;
    public final String username;
    public final String password;
    public final String apiurl;
    
    private Integer parseParam(String param) {
        try{
            int a = Integer.parseInt(param.trim());
            return a;
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private Config() {
        File file = new File("./api.conf");
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String text;
            while ((text = reader.readLine()) != null) {
                if (text.startsWith("#") || !text.contains("="))
                    continue;
                // System.out.println(text);
                text += " ";
                String[] params = text.split("=");
                Integer n;
                switch (params[0].trim().toLowerCase()) {
                    case "rpcip":
                        rpcUrl = params[1].trim();
                        break;
                    case "rpcport":
                        n = parseParam(params[1]);
                        if (n != null)
                            rpcPortNumber = n;
                        break;
                    case "rpcuser":
                        rpcUserName = params[1].trim();
                        break;
                    case "rpcpassword":
                        rpcPassword = params[1].trim();
                        break;
                    case "apiurl":
                        apiUrl = params[1].trim();
                        break;
                    case "apiport":
                        n = parseParam(params[1]);
                        if (n != null)
                            apiPortNumber = n;
                        break;
                        
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("config file not found. Default config written.");
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                writer.write("#default config\n");
                writer.write("rpcip="+rpcUrl+"\n");
                writer.write("rpcport="+rpcPortNumber+"\n");
                writer.write("rpcuser="+rpcUserName+"\n");
                writer.write("rpcpassword="+rpcPassword+"\n");
                writer.write("apiurl="+apiUrl+"\n");
                writer.write("apiport="+apiPortNumber+"\n");
                writer.close();
            } catch (IOException ex) {
                System.err.println("error in file write");
            }
        } catch (IOException e) {
            System.err.println("error in file read");
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                System.err.println("error in file close");
            }
        }
        
        rpcport = rpcPortNumber;
        apiport = apiPortNumber;
        rpcaddr = rpcUrl;
        username = rpcUserName;
        password = rpcPassword;
        apiurl = apiUrl;        
    }
    
    public static Config getConfig() {
        if (singleton == null)
            singleton = new Config();
        return singleton;
    }
    
    
}
