/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caddiecleaner;

import beansForJdbc.BeanBDAccess;
import static java.lang.Thread.sleep;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author hector
 */
public class CaddieCleaner {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        BeanBDAccess db = new BeanBDAccess("com.mysql.cj.jdbc.Driver", "jdbc:mysql://localhost:3306/bd_shopping", "hector", "WA0UH.nice.key");
        try {
            db.creerConnexionBD();
        }
        catch (Exception ex) {
            System.err.println("Database error: " + ex.getMessage());
        }
        
        while(true) {
            try {
                System.out.println("Cleaned!");
                db.executeRequeteMiseAJour("DELETE FROM users_products WHERE DATE_ADD(dateAjout, INTERVAL 30 MINUTE) < CURRENT_TIMESTAMP()");
                db.executeRequeteMiseAJour("DELETE FROM users_tickets WHERE DATE_ADD(dateAjout, INTERVAL 30 MINUTE) < CURRENT_TIMESTAMP()");
                sleep(60000);
            } catch (InterruptedException ex) {
                System.err.println("Interrupted error: " + ex.getMessage());
            } catch (Exception ex) {
                System.err.println("Error: " + ex.getMessage());
            }
        }
    }
    
}
