/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ProtocolePLAMAP;

import ProtocoleCHAMAP.ReponseCHAMAP;
import ProtocoleCHAMAP.RequeteCHAMAP;
import beansForJdbc.BeanBDAccess;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.sql.ResultSet;
import java.util.Vector;
import protocole.ConsoleServeur;
import protocole.Requete;

/**
 *
 * @author hector
 */
public class RequetePLAMAP implements Requete, Serializable {
    private static final long serialVersionUID = 6279354070353143569L;
    
    public static int LOGIN_CONT = 1;
    public static int GET_XY = 2;
    public static int SEND_WEIGHT = 3;
    public static int GET_LIST = 4;
    public static int SIGNAL_DEP = 5;
    
    private int type;
    private String chargeUtile;
    private ObjectOutputStream cli_oos;
    private ObjectInputStream cli_ois;
    private BufferedReader in;

    public RequetePLAMAP(String chu) {
        String[] t = chu.split("::");
        
        if("LOGIN_CONT".equals(t[0]))
            type = LOGIN_CONT;
        else if("GET_XY".equals(t[0]))
            type = GET_XY;
        else if("SEND_WEIGHT".equals(t[0]))
            type = SEND_WEIGHT;
        else if("GET_LIST".equals(t[0]))
            type = GET_LIST;
        else if("SIGNAL_DEP".equals(t[0]))
            type = SIGNAL_DEP;
        
        if(t.length >= 2)
            chargeUtile = chu.split("::")[1];
        else
            chargeUtile = "";
    }
    
    public RequetePLAMAP(int t, String chu) {
        type = t;
        chargeUtile = chu;
    }

    public String getChargeUtile() {
        return chargeUtile;
    }

    public int getType() {
        return type;
    }

    public BufferedReader getIn() {
        return in;
    }

    public void setIn(BufferedReader in) {
        this.in = in;
    }

    public void setCli_oos(ObjectOutputStream cli_oos) {
        this.cli_oos = cli_oos;
    }

    public void setCli_ois(ObjectInputStream cli_ois) {
        this.cli_ois = cli_ois;
    }
    
    @Override
    public Runnable createRunnable(Socket s, ConsoleServeur cs) {
        if(type == LOGIN_CONT) {
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
        BeanBDAccess db = new BeanBDAccess("com.mysql.cj.jdbc.Driver", "jdbc:mysql://localhost:3306/bd_mouvements", "hector", "WA0UH.nice.key");
        try {
            db.creerConnexionBD();
        }
        catch (Exception ex) {
            return;
        }
        
        boolean loggedIn = false;
        
        DataOutputStream out;
        try {
            out = new DataOutputStream(sock.getOutputStream());
        } catch(IOException e) {
            System.err.println("Erreur ? [" + e.getMessage() + "]");
            return;
        }
        RequetePLAMAP req = this;
        String rep;
        
        RequeteCHAMAP cli_req;
        ReponseCHAMAP cli_rep;
        
        while(true) {
            if(req.getType() == RequetePLAMAP.LOGIN_CONT) {
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début de Login_Cont : adresse distante = " + adresseDistante);
                // la charge utile est le nom et mot de passe
                String cu = getChargeUtile();
                
                if(!loggedIn) {
                    String[] parser = cu.split("  ");

                    if(parser.length >= 2) {
                        String user = parser[0];
                        String pass = parser[1];
                        cs.TraceEvenements(adresseDistante + "#Connexion de " + user + "; MDP = " + pass + "#" + Thread.currentThread().getName());
                        try {
                            ResultSet rs = db.executeRequeteSelection("SELECT pass FROM users WHERE name = '" + user + "'");
                            if(rs.next() && pass.equals(rs.getString("pass"))) {
                                System.out.println("Connexion réussie");
                                loggedIn = true;
                                rep = "LOGIN_CONT_OK";
                            }
                            else
                                rep = "WRONG_LOGIN";
                        } catch (Exception ex) {
                            rep = "SERVER_FAIL";
                        }
                    }
                    else
                        rep = "INVALID_FORMAT";
                }
                else
                    rep = "ALREADY_LOGGED_IN";
            }
            else if(req.getType() == RequetePLAMAP.GET_XY) {
                // Affichage des informations
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début de Get_XY : adresse distante = " + adresseDistante);
                // la charge utile est le nom et mot de passe
                String cu = req.getChargeUtile();

                if(loggedIn) {
                    String[] parser = cu.split("  ");

                    if(parser.length >= 5) {
                        String societe = parser[0];
                        String transEntrant = parser[1];
                        String container = parser[2];
                        String destination = parser[3];
                        String dateArrivee = parser[4];
                        String contenu = parser[5];
                        String capacite = parser[6];
                        String dangers = parser[7];
                        cs.TraceEvenements(adresseDistante + "#Obtenir emplacement pour " + container + "#" + Thread.currentThread().getName());
                        
                        try {
                            ResultSet rs = db.executeRequeteSelection("SELECT * FROM parc MINUS SELECT x, y FROM occupations WHERE dateDebut > CAST('" + dateArrivee + "' AS DATE) OR dateFin > CAST('" + dateArrivee + "' AS DATE)");
                            if(rs.next()) {
                                String x = rs.getString("x");
                                String y = rs.getString("y");
                                try {
                                    db.executeRequeteMiseAJour("INSERT INTO containers VALUES (" + container + ", '" + societe + "', '" + contenu + "', " + capacite + ", '" + dangers + "')");
                                } catch (Exception e) {
                                    try {
                                        db.executeRequeteMiseAJour("UPDATE containers SET proprietaire = '" + societe + "', contenu = '" + contenu + "', capacite = " + capacite + ", dangers = '" + dangers + "' WHERE id = " + container);
                                    } catch (Exception ex) {
                                        System.err.println("Erreur ? [" + ex.getMessage() + "]");
                                        rep = "SQL_ERROR";
                                    }
                                }
                                rs = db.executeRequeteSelection("SELECT * FROM mouvements WHERE dateDepart IS NULL AND container = " + container);
                                if(rs.next())
                                    rep = "CONTAINER_ALREADY_PRESENT";
                                else {
                                    try {
                                        db.executeRequeteMiseAJour("INSERT INTO mouvements (container, transEntrant, dateArrivee, poids, destination) VALUES (" + container + ", '" + transEntrant + "', CAST('" + dateArrivee + "' AS DATE), 0, " + destination + ")");
                                        rep = "GET_XY_OK::" + x + ", " + y;
                                    } catch (Exception ex) {
                                        System.err.println("Erreur ? [" + ex.getMessage() + "]");
                                        rep = "SQL_ERROR";
                                    }
                                }
                            }
                            else
                                rep = "NO_SPACE_LEFT";
                        } catch (Exception ex) {
                            rep = "SERVER_FAIL";
                        }
                    }
                    else
                        rep = "INVALID_FORMAT";
                }
                else
                    rep = "NOT_LOGGED_IN";
            }
            else if(req.getType() == RequetePLAMAP.SEND_WEIGHT) {
                // Affichage des informations
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début de Send_Weight : adresse distante = " + adresseDistante);
                // la charge utile est le nom et mot de passe
                String cu = req.getChargeUtile();

                if(loggedIn) {
                    String[] parser = cu.split("  ");

                    if(parser.length >= 3) {
                        String container = parser[0];
                        String emplacement = parser[1];
                        String poids = parser[2];
                        cs.TraceEvenements(adresseDistante + "#Enregistrement du poids pour " + container + "#" + Thread.currentThread().getName());
                        
                        
                        
                        rep = "SEND_WEIGHT_OK";
                    }
                    else
                        rep = "INVALID_FORMAT";
                }
                else
                    rep = "NOT_LOGGED_IN";
            }
            else if(req.getType() == RequetePLAMAP.GET_LIST) {
                // Affichage des informations
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début de Get_List : adresse distante = " + adresseDistante);
                // la charge utile est le nom et mot de passe
                String cu = req.getChargeUtile();

                if(loggedIn) {
                    String[] parser = cu.split("  ");

                    if(parser.length >= 3) {
                        String identifiant = parser[0];
                        String destination = parser[1];
                        String nbContainers = parser[2];
                        cs.TraceEvenements(adresseDistante + "#Liste des emplacements occupés pour " + destination + "#" + Thread.currentThread().getName());

                        try {
                            ResultSet rs = db.executeRequeteSelection("SELECT * FROM occupations WHERE container IN (SELECT container FROM mouvements WHERE destination = '" + destination + "') ORDER BY dateDebut ASC");
                            String out_cu = "";
                            while(rs.next()) {
                                out_cu += rs.getString("x") + "," + rs.getString("y");
                                out_cu += "  ";
                            }
                            rep = "GET_LIST_OK::"+out_cu;
                        } catch (Exception ex) {
                            rep = "SERVER_FAIL";
                        }
                    }
                    else
                        rep = "INVALID_FORMAT";
                }
                else
                    rep = "NOT_LOGGED_IN";
            }
            else if(req.getType() == RequetePLAMAP.SIGNAL_DEP) {
                // Affichage des informations
                String adresseDistante = sock.getRemoteSocketAddress().toString();
                System.out.println("Début de Signal_Dep : adresse distante = " + adresseDistante);
                // la charge utile est le nom et mot de passe
                String cu = req.getChargeUtile();

                if(loggedIn) {
                    String[] parser = cu.split("  ");

                    if(parser.length >= 3) {
                        String identifiant = parser[0];
                        Vector<String> containers = new Vector<>();
                        for(int i = 1; i < parser.length; i++)
                            containers.add(parser[i]);
                        cs.TraceEvenements(adresseDistante + "#Signal de départ pour " + identifiant + "#" + Thread.currentThread().getName());
                        
                        String cont_list = containers.get(0);
                        for(int i = 1; i < containers.size(); i++)
                            cont_list += ", " + containers.get(i);
                        
                        try {
                            cli_req = new RequeteCHAMAP(RequeteCHAMAP.MAKE_BILL, identifiant + "  " + cont_list);
                            
                            cli_oos.writeObject(cli_req);
                            cli_oos.flush();
                
                            cli_rep = (ReponseCHAMAP)cli_ois.readObject();
                            System.out.println("Requete lue par le serveur, instance de " + req.getClass().getName());
                            
                            if(cli_rep.getCode() == ReponseCHAMAP.MAKE_BILL_OK) {
                                db.executeRequeteMiseAJour("UPDATE occupations SET dateFin = CAST('" + java.time.LocalDate.now() + "' AS DATE) WHERE id IN (" + cont_list + ")");
                                db.executeRequeteMiseAJour("UPDATE mouvements SET transSortant = '" + identifiant + "', dateDepart = CAST('" + java.time.LocalDate.now() + "' AS DATE) WHERE dateDepart IS NULL AND container IN (" + cont_list + ")");
                                rep = "SIGNAL_DEP_OK";
                            }
                            else
                                rep = "BILLING_ERROR";
                        } catch (Exception ex) {
                            System.err.println("Erreur ? [" + ex.getMessage() + "]");
                            rep = "SERVER_FAIL";
                        }
                    }
                    else
                        rep = "INVALID_FORMAT";
                }
                else
                    rep = "NOT_LOGGED_IN";
            }
            else
                rep = "UNKNOWN_TYPE";
            
            try {
                rep += "\r\n";
                out.writeBytes(rep);
                out.flush();
                
                String chu = in.readLine();
                req = new RequetePLAMAP(chu.split("\r\n")[0]);
                System.out.println("Requete lue par le serveur, instance de " + req.getClass().getName());
            }
            catch(IOException e) {
                System.err.println("Erreur réseau ? [" + e.getMessage() + "]");
                break;
            }
        }
        
        try {
            sock.close();
        } catch (IOException e) {
            System.err.println("Erreur ? [" + e.getMessage() + "]");
        }
    }
    
    private void traiteRequeteLoggedOut(Socket sock, ConsoleServeur cs) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
            out.writeBytes("NOT_LOGGED_IN");
            out.flush();
            sock.close();
        }
        catch(IOException e) {
            System.err.println("Erreur ? [" + e.getMessage() + "]");
        }
    }
    
}
