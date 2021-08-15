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
        
    }
    
}
