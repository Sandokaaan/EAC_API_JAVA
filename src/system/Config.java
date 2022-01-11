/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package system;

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
    public final String coinName;
    public final String coinTicker;    
    public final int protocolVersion;
    public final String rpcUrl;
    public final int rpcPortNumber;
    public final String rpcUserName;
    public final String rpcPassword;
    public final String apiUrl;
    public final int apiPortNumber;
    public final boolean usessl;
    public final String sslFileName;
    public final String sslPassword;
    public final boolean withExplorer;
    public final String p2pUrl;
    public final int p2pPortNumber;
    public final int p2pMagic;
    public final String p2pPeerName;
    public final int reconnectDelay;
    public final String dbDriver;
    public final String dbPrefix;
    public final String dbName;
    public final String dbUser;
    public final String dbPassword;
    public final String docPath;
    
    private static Integer parseParam(String param) {
        try{
            int a = Integer.parseInt(param.trim());
            return a;
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private static String stringParam(String param, String value) {
        return (value!=null)?value:param;
    }
    
    private static int intParam(int param, String value) {
        Integer tmp = parseParam(value);
        return (value!=null && tmp!=null)?tmp:param;
    }

    private static boolean boolParam(boolean param, String value) {
        Integer tmp = parseParam(value);
        return (value!=null && tmp!=null)?(tmp!=0):param;
    }
    
    
    @SuppressWarnings("ConvertToTryWithResources")
    private Config() {
        // default values
        String _coinName = "Earthcoin";
        String _coinTicker = "EAC";
        int _protocolVersion = 70018;
        String _rpcUrl = "127.0.0.1";
        int _rpcPortNumber = 15678;
        String _rpcUserName = "username";
        String _rpcPassword = "password";
        String _apiUrl = "127.0.0.1";
        int _apiPortNumber = 9000;
        boolean _usessl = false;
        String _sslFileName = "./cert.jks";
        String _sslPassword = "password";
        boolean _withExplorer = true;
        String _p2pUrl = "127.0.0.1";
        int _p2pPortNumber = 35677;
        int _p2pMagic = -34481216;
        String _p2pPeerName = "Java API Server";
        int _reconnectDelay = 5000;
        String _dbDriver = "org.h2.Driver";
        String _dbPrefix = "jdbc:h2:./db/";
        String _dbName = "database";
        String _dbUser = "sa";
        String _dbPassword = "";
        String _docPath = "./doc/english.json";
        // load config from a file
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
                String param = params[0].trim().toLowerCase();
                String value = (params.length==1)?"":params[1].trim();
                switch (params[0].trim().toLowerCase()) {
                    case "coin": _coinName=stringParam(_coinName, value); break;
                    case "ticker": _coinTicker=stringParam(_coinTicker, value); break;
                    case "protocolversion": _protocolVersion=intParam(_protocolVersion, value); break;
                    case "rpcip": _rpcUrl=stringParam(_rpcUrl, value); break;
                    case "rpcport": _rpcPortNumber=intParam(_rpcPortNumber, value); break;
                    case "rpcuser": _rpcUserName=stringParam(_rpcUserName, value); break;
                    case "rpcpassword": _rpcPassword=stringParam(_rpcPassword, value); break;
                    case "apiurl": _apiUrl=stringParam(_apiUrl, value); break;
                    case "apiport": _apiPortNumber=intParam(_apiPortNumber, value); break;
                    case "usessl": _usessl=boolParam(_usessl, value); break;
                    case "sslcertpath": _sslFileName=stringParam(_sslFileName, value); break;
                    case "sslcertpassword": _sslPassword=stringParam(_sslPassword, value); break;
                    case "explorer": _withExplorer=boolParam(_withExplorer, value); break;
                    case "p2pip": _p2pUrl=stringParam(_p2pUrl, value); break;
                    case "p2pport": _p2pPortNumber=intParam(_p2pPortNumber, value); break;
                    case "p2pmagic": _p2pMagic=intParam(_p2pMagic, value); break;
                    case "p2ppeername": _p2pPeerName=stringParam(_p2pPeerName, value); break;
                    case "reconnectdelay": _reconnectDelay=intParam(_reconnectDelay, value); break;
                    case "dbdriver": _dbDriver=stringParam(_dbDriver, value); break;
                    case "dbprefix": _dbPrefix=stringParam(_dbPrefix, value); break;
                    case "dbname": _dbName=stringParam(_dbName, value); break;
                    case "dbuser": _dbUser=stringParam(_dbUser, value); break;
                    case "dbpassword": _dbPassword=stringParam(_dbPassword, value); break;
                    case "docpath": _docPath=stringParam(_docPath, value); break;                    
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("config file not found. Default config written.");
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                writer.write("#Default config\n\n");
                writer.write("#Coin name\n");
                writer.write("coin="+_coinName+"\n\n");                
                writer.write("#Coin ticker\n");
                writer.write("ticker="+_coinTicker+"\n\n");  
                writer.write("#Protocol version,  see source code of your coin\n");
                writer.write("protocolversion="+_protocolVersion+"\n\n");  
                writer.write("#IP address for RPC requests, usually localhost\n");
                writer.write("rpcip="+_rpcUrl+"\n\n");
                writer.write("#Port number of RPC server, see your node config file\n");
                writer.write("rpcport="+_rpcPortNumber+"\n\n");
                writer.write("#User name for RPC setver, see your node config file\n");
                writer.write("rpcuser="+_rpcUserName+"\n\n");
                writer.write("#Password for RPC setver, see your node config file\n");
                writer.write("rpcpassword="+_rpcPassword+"\n\n");
                writer.write("#URL of your API server, use IP or domain name\n");
                writer.write("apiurl="+_apiUrl+"\n\n");
                writer.write("#Port number of your API server\n");
                writer.write("apiport="+_apiPortNumber+"\n\n");
                writer.write("#API run over SSL (1) or unsecured (0)\n");
                writer.write("usessl="+(_usessl?1:0)+"\n\n");
                writer.write("#Path to your SSL certificate, if applicable\n");
                writer.write("sslcertpath="+_sslFileName+"\n\n");
                writer.write("#Password for your SSL certificate, if applicable\n");
                writer.write("sslcertpassword="+_sslPassword+"\n\n");
                writer.write("#Use build-in block explorer (1) or API only(0)\n");
                writer.write("explorer="+(_withExplorer?1:0)+"\n\n");
                writer.write("#IP address for P2P connection, usually localhost\n");
                writer.write("p2pip="+_p2pUrl+"\n\n");
                writer.write("#Port number of your P2P peer, see your node config file\n");
                writer.write("p2pport="+_p2pPortNumber+"\n\n");
                writer.write("#Magic number for connection to P2P network, see source code of your coin\n");
                writer.write("p2pmagic="+_p2pMagic+"\n\n");
                writer.write("#Name of the client in the P2P network, arbitrary\n");
                writer.write("p2ppeername="+_p2pPeerName+"\n\n");
                writer.write("#Delay (in miliseconds) for next reconnect attemp on p2p connection lost\n");
                writer.write("reconnectdelay="+_reconnectDelay+"\n\n");
                writer.write("#Database driver. Do not change, if not understand.\n");
                writer.write("dbdriver="+_dbDriver+"\n\n");                
                writer.write("#Database prefix. Do not change, if not understand.\n");
                writer.write("dbprefix="+_dbPrefix+"\n\n");
                writer.write("#Database name.\n");
                writer.write("dbname="+_dbName+"\n\n");                
                writer.write("#Database username.\n");
                writer.write("dbuser="+_dbUser+"\n\n");                
                writer.write("#Database password.\n");
                writer.write("dbpassword="+_dbPassword+"\n\n");                
                writer.write("#Absolute or relative path to the API documentation file.\n "
                            + "#The file must contain a valid json structure.\n "
                            + "#Feel free to use a translation to your language.\n");
                writer.write("docpath="+_docPath+"\n\n");                
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
        // copy valueas to properties
        coinName = _coinName;
        coinTicker = _coinTicker;
        protocolVersion = _protocolVersion;
        rpcUrl = _rpcUrl; 
        rpcPortNumber = _rpcPortNumber;
        rpcUserName = _rpcUserName;
        rpcPassword = _rpcPassword;
        apiUrl = _apiUrl;
        apiPortNumber = _apiPortNumber;
        usessl = _usessl;
        sslFileName = _sslFileName;
        sslPassword = _sslPassword;
        withExplorer = _withExplorer;
        p2pUrl = _p2pUrl;
        p2pPortNumber = _p2pPortNumber;
        p2pMagic = _p2pMagic;
        p2pPeerName = _p2pPeerName;
        reconnectDelay = _reconnectDelay;
        dbDriver = _dbDriver;
        dbPrefix = _dbPrefix;
        dbName = _dbName;
        dbUser = _dbUser;
        dbPassword = _dbPassword;
        docPath = _docPath;
    }
    
    public static Config getConfig() {
        if (singleton == null)
            singleton = new Config();
        return singleton;
    }
    
    
}
