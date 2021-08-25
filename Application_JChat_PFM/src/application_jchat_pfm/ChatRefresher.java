/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package application_jchat_pfm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import javax.swing.table.DefaultTableModel;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 *
 * @author hector
 */
public class ChatRefresher extends Thread {
    private MulticastSocket socketClient;
    private DefaultTableModel dtm;
    
    public ChatRefresher(MulticastSocket socketClient, DefaultTableModel dtm) {
        this.socketClient = socketClient;
        this.dtm = dtm;
    }
    
    public void run() {
        int bufLen = 256;
        byte[] buf = new byte[bufLen];
        
        while(!isInterrupted()) {
            try {
                // réception d'une requete
                DatagramPacket paquet = new DatagramPacket(buf, bufLen);
                socketClient.receive(paquet);
                InetAddress adresse = paquet.getAddress();
                int port = paquet.getPort();
                System.out.println("Requete reçue de " + adresse + " - port: " + port);

                byte[] digest = new byte[20];
                byte[] cu = new byte[256];

                ByteArrayInputStream bais = new ByteArrayInputStream(buf);
                int type = (int)bais.read();
                bais.read(digest, 0, 20);
                int length = (int)bais.read();
                bais.read(cu, 0, length);
                String chargeUtile = new String(cu);

                // confection d'un digest local
                Security.addProvider(new BouncyCastleProvider());
                MessageDigest md = MessageDigest.getInstance("SHA-1", "BC");
                md.update(chargeUtile.getBytes());
                
                byte[] msgDLocal = md.digest();
                
                if(MessageDigest.isEqual(digest, msgDLocal)) {
                    Object[] row = new Object[]{digest, chargeUtile};
                    dtm.addRow(row);
                }
            } catch (IOException ex) {
                System.err.println("Erreur ? [" + ex.getMessage() + "]");
            } catch (NoSuchAlgorithmException | NoSuchProviderException ex) {
                System.err.println("Erreur digest ? [" + ex.getMessage() + "]");
            }
        }
    }
}
