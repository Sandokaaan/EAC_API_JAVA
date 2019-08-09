/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package apiextension;

import apiserver.ApiListen;
import java.util.Scanner;

/**
 *
 * @author Vratislav Bednařík
 */
public class ApiExtension {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        Scanner sc = new Scanner(System.in);
        
        ApiListen api = new ApiListen();
        api.start();
        
        while (true) {
            String s = sc.nextLine();
            if (s.equals("konec"))
                break;
        }
        
        api.close();
        
        /*
        RpcClient client = new RpcClient("127.0.0.1", 20000,"userEAC","J0st4St4ryhoP4r4n01dn1h0AJ3st3KT0muS1l3nyh0");
        
        for (int i=0; i< 100; i++)
        {
            String rts = client.query("getblockhash", i);
            String raw = client.query("getblock", rts, true);
            System.out.println(i + " - " + rts + "\n" + raw + "\n\n");
        }*/
    }
    
}
