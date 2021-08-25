/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ProtocoleBISAMAP;

import beansForJdbc.BeanBDAccess;
import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.X509EncodedKeySpec;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import protocole.ConsoleServeur;
import protocole.Requete;

/**
 *
 * @author hector
 */
public class RequeteBISAMAP implements Requete, Serializable {
    private static final long serialVersionUID = 6279354070353143569L;
    
    public static String codeProvider = "BC"; //CryptixCrypto";
    
    public static int LOGIN = 1;
    public static int GET_NEXT_BILL = 2;
    public static int VALIDATE_BILL = 3;
    public static int LIST_BILLS = 4;
    public static int SEND_BILLS = 5;
    public static int REC_PAY = 6;
    public static int LIST_WAITING = 7;
    
    private int type;
    private String chargeUtile;
    private byte[] digest;
    private byte[] signature;
    private Socket socketClient;
    private ObjectInputStream ois;

    public RequeteBISAMAP(int t, String chu) {
        type = t;
        chargeUtile = chu;
    }
    
    public RequeteBISAMAP(int t, String chu, byte[] dig) {
        type = t;
        chargeUtile = chu;
        digest = dig;
    }
    
    public RequeteBISAMAP(int t, String chu, byte[] dig, byte[] sig) {
        type = t;
        chargeUtile = chu;
        digest = dig;
        signature = sig;
    }
    
    public RequeteBISAMAP(int t, String chu, Socket s) {
        type = t;
        chargeUtile = chu;
        socketClient = s;
    }

    public String getChargeUtile() {
        return chargeUtile;
    }

    public byte[] getDigest() {
        return digest;
    }

    public void setDigest(byte[] digest) {
        this.digest = digest;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }
    
    public int getType() {
        return type;
    }

    public ObjectInputStream getOis() {
        return ois;
    }

    public void setOis(ObjectInputStream ois) {
        this.ois = ois;
    }
    
    @Override
    public Runnable createRunnable(Socket s, ConsoleServeur cs) {
        if(type == LOGIN) {
            return new Runnable() {
                public void run() {
                    traiteRequeteLogin(s, cs);
                }
            };
        }
        else {
            return new Runnable() {
                public void run() {
                    traiteRequeteLoggedOut(s, cs);
                }
            };
        }
    }
    
    private void traiteRequeteLogin(Socket sock, ConsoleServeur cs) {
        BeanBDAccess db = new BeanBDAccess("com.mysql.cj.jdbc.Driver", "jdbc:mysql://localhost:3306/bd_compta", "hector", "WA0UH.nice.key");
        try {
            db.creerConnexionBD();
        }
        catch (Exception ex) {
            return;
        }
        
        String session = null;
        
        ObjectOutputStream oos = null;
        RequeteBISAMAP req = this;
        ReponseBISAMAP rep = null;
        
        SecretKey cle = null;
        SecretKey cle_hmac = null;
        PublicKey cle_publique = null;
        
        try {
            oos = new ObjectOutputStream(sock.getOutputStream());
        } catch (IOException ex) {
            System.err.println("Erreur stream ? [" + ex.getMessage() + "]");
            return;
        }
        
        while(true) {
            if(req.getType() == RequeteBISAMAP.LOGIN) {
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début de Login : adresse distante = " + adresseDistante);
                // la charge utile est le nom
                String cu = req.getChargeUtile();
                
                if(session == null) {
                    String[] parser = cu.split("  ");
                    
                    if(parser.length >= 3) {
                        String user = parser[0];
                        String temps = parser[1];
                        String alea = parser[2];
                        
                        cs.TraceEvenements(adresseDistante + "#Connexion de " + user + "#" + Thread.currentThread().getName());
                        try {
                            ResultSet rs = db.executeRequeteSelection("SELECT * FROM personnel WHERE login = '" + user + "'");
                            if(rs.next())
                            {
                                String pass = rs.getString("password");

                                // confection d'un digest local
                                Security.addProvider(new BouncyCastleProvider());
                                MessageDigest md = MessageDigest.getInstance("SHA-1", codeProvider);
                                md.update(user.getBytes());
                                md.update(pass.getBytes());
                                md.update(temps.getBytes());
                                md.update(alea.getBytes());
                                
                                byte[] msgDLocal = md.digest();

                                if(MessageDigest.isEqual(req.getDigest(), msgDLocal)) {
                                    BigInteger n = new BigInteger("" + (int)(Math.random() * 100000000));
                                    BigInteger p = new BigInteger("0");

                                    while (n.compareTo(p) > 0)
                                        p = new BigInteger("" + (int)(Math.random() * 100000000));

                                    int a = (int) (Math.random() * 100);
                                    BigInteger pubkey_a = n.pow(a).remainder(p);

                                    rep = new ReponseBISAMAP(ReponseBISAMAP.LOGIN_OK, n.toString() + "  " + p.toString() + "  " + pubkey_a.toString());
                                    
                                    oos.writeObject(rep);
                                    oos.flush();

                                    req = (RequeteBISAMAP)ois.readObject();
                                    System.out.println("Requete lue par le serveur, instance de " + req.getClass().getName());
                                    
                                    BigInteger pubkey_b = new BigInteger(req.getChargeUtile());
                                    
                                    String key_a = String.format("%8s", pubkey_b.pow(a).remainder(p).toString()).replace(" ", "0");
                                    
                                    System.out.println(" *** Clé obtenue = " + key_a);
                                    
                                    cle = new SecretKeySpec(key_a.getBytes(), "DES");
                                    
                                    Cipher chiffrement = Cipher.getInstance("DES/ECB/PKCS5Padding", RequeteBISAMAP.codeProvider);
                                    chiffrement.init(Cipher.DECRYPT_MODE, cle);

                                    byte[] texteCrypte = req.getDigest();
                                    System.out.println(" *** Texte crypté = " + new String(texteCrypte));
                                    byte[] texteClair = chiffrement.doFinal(texteCrypte);
                                    System.out.println(" *** Texte clair = " + new String(texteClair));
                                    
                                    cle_hmac = new SecretKeySpec(texteClair, "DES");
                                    
                                    texteCrypte = req.getSignature();
                                    System.out.println(" *** Texte crypté = " + new String(texteCrypte));
                                    texteClair = chiffrement.doFinal(texteCrypte);
                                    System.out.println(" *** Texte clair = " + new String(texteClair));
                                    
                                    cle_publique = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(texteClair));
                                    
                                    chiffrement = Cipher.getInstance("DES/ECB/PKCS5Padding", codeProvider);
                                    chiffrement.init(Cipher.ENCRYPT_MODE, cle);
                                    
                                    texteClair = "Hello world!".getBytes();
                                    texteCrypte = chiffrement.doFinal(texteClair);
                                    System.out.println(" *** Texte crypté = " + new String(texteCrypte));
                                    
                                    session = rs.getString("id");
                                    rep = new ReponseBISAMAP(ReponseBISAMAP.LOGIN_OK, "", texteCrypte);
                                }
                                else
                                    rep = new ReponseBISAMAP(ReponseBISAMAP.WRONG_LOGIN, null);
                            }
                            else
                                rep = new ReponseBISAMAP(ReponseBISAMAP.WRONG_LOGIN, null);
                        } catch (Exception ex) {
                            rep = new ReponseBISAMAP(ReponseBISAMAP.SERVER_FAIL, null);
                        }
                    }
                    else
                        rep = new ReponseBISAMAP(ReponseBISAMAP.INVALID_FORMAT, null);
                }
                else
                    rep = new ReponseBISAMAP(ReponseBISAMAP.ALREADY_LOGGED_IN, null);
            }
            else if (req.getType() == RequeteBISAMAP.GET_NEXT_BILL) {
                // Affichage des informations
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début de Get_Next_Bill : adresse distante = " + adresseDistante);

                if(session != null) {
                    cs.TraceEvenements(adresseDistante + "#Facture la plus ancienne non validée#" + Thread.currentThread().getName());
                    try {
                        ResultSet rs = db.executeRequeteSelection("SELECT * FROM factures WHERE validee = 0 ORDER BY periode ASC");
                        if(rs.next())
                        {
                            String chu = rs.getString("id");
                            chu += "  " + rs.getString("societe");
                            chu += "  " + rs.getString("periode");
                            chu += "  " + rs.getString("validee");
                            chu += "  " + rs.getString("comptable");
                            chu += "  " + rs.getString("envoyee");
                            chu += "  " + rs.getString("payee");
                            
                            Cipher chiffrement = Cipher.getInstance("DES/ECB/PKCS5Padding", codeProvider);
                            chiffrement.init(Cipher.ENCRYPT_MODE, cle);
                            byte[] texteClair = chu.getBytes();
                            byte[] texteCrypte = chiffrement.doFinal(texteClair);
                            System.out.println(" *** Texte crypté = " + new String(texteCrypte));
                            
                            rep = new ReponseBISAMAP(ReponseBISAMAP.GET_NEXT_BILL_OK, null, texteCrypte);
                        }
                        else
                            rep = new ReponseBISAMAP(ReponseBISAMAP.NO_BILL, null);
                    } catch (Exception ex) {
                        rep = new ReponseBISAMAP(ReponseBISAMAP.SERVER_FAIL, null);
                    }
                }
                else
                    rep = new ReponseBISAMAP(ReponseBISAMAP.NOT_LOGGED_IN, null);
            }
            else if (req.getType() == RequeteBISAMAP.VALIDATE_BILL) {
                // Affichage des informations
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début de Validate_Bill : adresse distante = " + adresseDistante);
                // la charge utile est le nom et mot de passe
                String cu = req.getChargeUtile();
                
                if(session != null) {
                    try {
                        Signature s = Signature.getInstance ("SHA1withRSA", codeProvider);
                        s.initVerify(cle_publique);
                        System.out.println("Hachage du message");
                        s.update(cu.getBytes());
                        System.out.println("Verification de la signature construite");

                        if(s.verify(req.getSignature())) {
                            String[] parser = cu.split("  ");

                            if(parser.length >= 2) {
                                String operation = parser[0];
                                String facture = parser[1];

                                if("V".equals(operation)) {
                                    cs.TraceEvenements(adresseDistante + "#Validation de " + facture + "#" + Thread.currentThread().getName());
                                    db.executeRequeteMiseAJour("UPDATE factures SET validee = 1, comptable = " + session + " WHERE id = " + facture);
                                    rep = new ReponseBISAMAP(ReponseBISAMAP.VALIDATE_BILL_OK, null);
                                }
                                else {
                                    cs.TraceEvenements(adresseDistante + "#Invalidation de " + facture + "#" + Thread.currentThread().getName());
                                    db.executeRequeteMiseAJour("DELETE FROM factures WHERE id = " + facture);
                                    rep = new ReponseBISAMAP(ReponseBISAMAP.VALIDATE_BILL_OK, null);
                                }
                            }
                        }
                        else
                            rep = new ReponseBISAMAP(ReponseBISAMAP.BAD_SIGNATURE, null);
                    } catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException | NoSuchProviderException ex) {
                        rep = new ReponseBISAMAP(ReponseBISAMAP.SERVER_FAIL, null);
                    } catch (Exception ex) {
                        rep = new ReponseBISAMAP(ReponseBISAMAP.SERVER_FAIL, null);
                    }
                }
                else
                    rep = new ReponseBISAMAP(ReponseBISAMAP.NOT_LOGGED_IN, null);
            }
            else if (req.getType() == RequeteBISAMAP.LIST_BILLS) {
                // Affichage des informations
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début de List_Bills : adresse distante = " + adresseDistante);
                // la charge utile est le nom et mot de passe
                String cu = req.getChargeUtile();

                if(session != null) {
                    try {
                        Signature s = Signature.getInstance ("SHA1withRSA", codeProvider);
                        s.initVerify(cle_publique);
                        System.out.println("Hachage du message");
                        s.update(cu.getBytes());
                        System.out.println("Verification de la signature construite");

                        if(s.verify(req.getSignature())) {
                            String[] parser = cu.split("  ");

                            if(parser.length >= 3) {
                                String societe = parser[0];
                                String debut = parser[1];
                                String fin = parser[2];

                                cs.TraceEvenements(adresseDistante + "#Liste des factures de la société " + societe + " entre " + debut + " et " + fin + "#" + Thread.currentThread().getName());

                                ResultSet rs = db.executeRequeteSelection("SELECT * FROM factures WHERE periode >= CAST('" + debut + "-01' AS DATE) AND periode <= CAST('" + fin + "-01' AS DATE) AND societe = '" + societe + "'");
                                if(rs.next())
                                {
                                    String chu = rs.getString("id");
                                    chu += "  " + rs.getString("societe");
                                    chu += "  " + rs.getString("periode");
                                    chu += "  " + rs.getString("validee");
                                    chu += "  " + rs.getString("comptable");
                                    chu += "  " + rs.getString("envoyee");
                                    chu += "  " + rs.getString("payee");

                                    while(rs.next()){
                                        chu += "::" + rs.getString("id");
                                        chu += "  " + rs.getString("societe");
                                        chu += "  " + rs.getString("periode");
                                        chu += "  " + rs.getString("validee");
                                        chu += "  " + rs.getString("comptable");
                                        chu += "  " + rs.getString("envoyee");
                                        chu += "  " + rs.getString("payee");
                                    }

                                    Cipher chiffrement = Cipher.getInstance("DES/ECB/PKCS5Padding", codeProvider);
                                    chiffrement.init(Cipher.ENCRYPT_MODE, cle);
                                    byte[] texteClair = chu.getBytes();
                                    byte[] texteCrypte = chiffrement.doFinal(texteClair);
                                    System.out.println(" *** Texte crypté = " + new String(texteCrypte));

                                    rep = new ReponseBISAMAP(ReponseBISAMAP.LIST_BILLS_OK, null, texteCrypte);
                                }
                                else
                                    rep = new ReponseBISAMAP(ReponseBISAMAP.NOT_LOGGED_IN, null);
                            }
                            else
                                rep = new ReponseBISAMAP(ReponseBISAMAP.INVALID_FORMAT, null);
                        }
                        else
                            rep = new ReponseBISAMAP(ReponseBISAMAP.BAD_SIGNATURE, null);
                    } catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException | NoSuchProviderException ex) {
                        rep = new ReponseBISAMAP(ReponseBISAMAP.SERVER_FAIL, null);
                    } catch (Exception ex) {
                        rep = new ReponseBISAMAP(ReponseBISAMAP.SERVER_FAIL, null);
                    }
                }
                else
                    rep = new ReponseBISAMAP(ReponseBISAMAP.NOT_LOGGED_IN, null);
            }
            else if (req.getType() == RequeteBISAMAP.SEND_BILLS) {
                // Affichage des informations
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début de Send_Bills : adresse distante = " + adresseDistante);
                // la charge utile est le nom et mot de passe
                String cu = req.getChargeUtile();

                if(session != null) {
                    try {
                        Signature s = Signature.getInstance ("SHA1withRSA", codeProvider);
                        s.initVerify(cle_publique);
                        System.out.println("Hachage du message");
                        s.update(cu.getBytes());
                        System.out.println("Verification de la signature construite");

                        if(s.verify(req.getSignature())) {
                            cs.TraceEvenements(adresseDistante + "#Envoi de factures#" + Thread.currentThread().getName());
                            if(!"".equals(cu)) {
                                String[] parser = cu.split("  ");
                                String factures = parser[0];
                                for(int i = 1; i < parser.length; i++)
                                    factures += ", " + parser[i];

                                db.executeRequeteMiseAJour("UPDATE factures SET envoyee = 1 WHERE validee = 1 AND comptable = " + session + " AND id NOT IN (" + factures + ")");
                                rep = new ReponseBISAMAP(ReponseBISAMAP.SEND_BILLS_OK, null);
                            }
                            else {
                                db.executeRequeteMiseAJour("UPDATE factures SET envoyee = 1 WHERE validee = 1 AND comptable = " + session);
                                rep = new ReponseBISAMAP(ReponseBISAMAP.SEND_BILLS_OK, null);
                            }
                        }
                        else
                            rep = new ReponseBISAMAP(ReponseBISAMAP.BAD_SIGNATURE, null);
                    } catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException | NoSuchProviderException ex) {
                        rep = new ReponseBISAMAP(ReponseBISAMAP.SERVER_FAIL, null);
                    } catch (Exception ex) {
                        System.err.println(ex.getMessage());
                        rep = new ReponseBISAMAP(ReponseBISAMAP.SERVER_FAIL, null);
                    }
                }
                else
                    rep = new ReponseBISAMAP(ReponseBISAMAP.NOT_LOGGED_IN, null);
            }
            else if (req.getType() == RequeteBISAMAP.REC_PAY) {
                // Affichage des informations
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début de Rec_Pay : adresse distante = " + adresseDistante);
                // la charge utile est le nom et mot de passe

                if(session != null) {
                    try {
                        // confection d'un HMAC local
                        Mac hlocal = Mac.getInstance("HMAC-MD5", codeProvider);
                        hlocal.init(cle_hmac);
                        System.out.println("Hachage du message");
                        hlocal.update(req.getDigest());
                        System.out.println("Verification des HMACS");
                        byte[] hlocalb = hlocal.doFinal();
                        
                        if (MessageDigest.isEqual(req.getSignature(), hlocalb)) {
                            System.out.println("Le message a été authentifié");
                            Cipher chiffrement = Cipher.getInstance("DES/ECB/PKCS5Padding", RequeteBISAMAP.codeProvider);
                            chiffrement.init(Cipher.DECRYPT_MODE, cle);

                            byte[] texteCrypte = req.getDigest();
                            System.out.println(" *** Texte crypté = " + new String(texteCrypte));
                            byte[] texteClair = chiffrement.doFinal(texteCrypte);
                            System.out.println(" *** Texte clair = " + new String(texteClair));

                            String facture = new String(texteClair).split(" ")[0];
                            cs.TraceEvenements(adresseDistante + "#Enregistrement du paiement pour " + facture + "#" + Thread.currentThread().getName());

                            db.executeRequeteMiseAJour("UPDATE factures SET payee = 1 WHERE id = " + facture);
                            rep = new ReponseBISAMAP(ReponseBISAMAP.REC_PAY_OK, null);
                        }
                        else {
                            System.out.println("Le message n'a pas été authentifié");
                            rep = new ReponseBISAMAP(ReponseBISAMAP.BAD_SIGNATURE, null);
                        }
                    } catch (Exception ex) {
                        rep = new ReponseBISAMAP(ReponseBISAMAP.SERVER_FAIL, null);
                    }
                }
                else
                    rep = new ReponseBISAMAP(ReponseBISAMAP.NOT_LOGGED_IN, null);
            }
            else if (req.getType() == RequeteBISAMAP.LIST_WAITING) {
                // Affichage des informations
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début de List_Waiting : adresse distante = " + adresseDistante);
                // la charge utile est le nom et mot de passe
                String cu = req.getChargeUtile();

                if(session != null) {
                    try {
                        Signature s = Signature.getInstance ("SHA1withRSA", codeProvider);
                        s.initVerify(cle_publique);
                        System.out.println("Hachage du message");
                        s.update(cu.getBytes());
                        System.out.println("Verification de la signature construite");

                        if(s.verify(req.getSignature())) {
                            String[] parser = cu.split("  ");
                            String indic = cu;
                            String societe;
                            if(parser.length >= 2) {
                                indic = parser[0];
                                societe = parser[1];
                            }

                            cs.TraceEvenements(adresseDistante + "#Liste des factures non payées#" + Thread.currentThread().getName());

                            ResultSet rs = db.executeRequeteSelection("SELECT * FROM factures WHERE payee = 0");
                            if(rs.next())
                            {
                                String chu = rs.getString("id");
                                chu += "  " + rs.getString("societe");
                                chu += "  " + rs.getString("periode");
                                chu += "  " + rs.getString("validee");
                                chu += "  " + rs.getString("comptable");
                                chu += "  " + rs.getString("envoyee");
                                chu += "  " + rs.getString("payee");

                                while(rs.next()){
                                    chu += "::" + rs.getString("id");
                                    chu += "  " + rs.getString("societe");
                                    chu += "  " + rs.getString("periode");
                                    chu += "  " + rs.getString("validee");
                                    chu += "  " + rs.getString("comptable");
                                    chu += "  " + rs.getString("envoyee");
                                    chu += "  " + rs.getString("payee");
                                }

                                Cipher chiffrement = Cipher.getInstance("DES/ECB/PKCS5Padding", codeProvider);
                                chiffrement.init(Cipher.ENCRYPT_MODE, cle);
                                byte[] texteClair = chu.getBytes();
                                byte[] texteCrypte = chiffrement.doFinal(texteClair);
                                System.out.println(" *** Texte crypté = " + new String(texteCrypte));

                                rep = new ReponseBISAMAP(ReponseBISAMAP.LIST_WAITING_OK, null, texteCrypte);
                            }
                            else
                                rep = new ReponseBISAMAP(ReponseBISAMAP.SERVER_FAIL, null);
                        }
                        else
                            rep = new ReponseBISAMAP(ReponseBISAMAP.BAD_SIGNATURE, null);
                    } catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException | NoSuchProviderException ex) {
                        rep = new ReponseBISAMAP(ReponseBISAMAP.SERVER_FAIL, null);
                    } catch (Exception ex) {
                        rep = new ReponseBISAMAP(ReponseBISAMAP.SERVER_FAIL, null);
                    }
                }
                else
                    rep = new ReponseBISAMAP(ReponseBISAMAP.NOT_LOGGED_IN, null);
            }
            else
                rep = new ReponseBISAMAP(ReponseBISAMAP.UNKNOWN_TYPE, null);

            try {
                oos.writeObject(rep);
                oos.flush();
                
                req = (RequeteBISAMAP)ois.readObject();
                System.out.println("Requete lue par le serveur, instance de " + req.getClass().getName());
            }
            catch (IOException | ClassNotFoundException e) {
                System.err.println("Erreur ? [" + e.getMessage() + "]");
                break;
            }
        }
        
        try {
            sock.close();
        } catch (IOException e) {
            System.err.println("Erreur socket ? [" + e.getMessage() + "]");
        }
    }
    
    private void traiteRequeteLoggedOut(Socket sock, ConsoleServeur cs) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
            oos.writeObject(new ReponseBISAMAP(ReponseBISAMAP.NOT_LOGGED_IN, null));
            oos.flush();
            sock.close();
        }
        catch(IOException e) {
            System.err.println("Erreur ? [" + e.getMessage() + "]");
        }
    }
}
