/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package system;

import apiextension.Config;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 *
 * @author virtu
 */
public class ServerSocketSSL{   

    private static SSLServerSocketFactory ssf = null;
    
    private static void initKeyStore() throws KeyStoreException, FileNotFoundException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, KeyManagementException {
        KeyStore ks = KeyStore.getInstance("JKS");
        String fn = Config.getConfig().sslFileName;
        char[] pw = Config.getConfig().sslPassword.toCharArray();
        FileInputStream fis = new FileInputStream(fn);
        ks.load(fis, pw);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, pw);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509"); 
        tmf.init(ks);
        SSLContext sc = SSLContext.getInstance("TLS"); 
        TrustManager[] trustManagers = tmf.getTrustManagers(); 
        sc.init(kmf.getKeyManagers(), trustManagers, null);
        ssf = sc.getServerSocketFactory();
    }       
    
    public static SSLServerSocket openSSLServerSocket(int portNumber) {
        if (ssf == null)
           try {
               initKeyStore();
           } catch (Exception ex) {
               System.err.println("SSL initialization failed");
               System.err.println(ex.getMessage());
               return null;
           }
        try {   
            return (SSLServerSocket) ssf.createServerSocket(portNumber);
        } catch (IOException ex) {
            System.err.println("Open SSL failed");
            System.err.println(ex.getMessage());
            return null;
        }
    }
}
