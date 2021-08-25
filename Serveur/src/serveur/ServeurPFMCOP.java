/*
 * Copyright (C) 2020 hector
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package serveur;

import ProtocolePFMCOP.ReponsePFMCOP;
import ProtocolePFMCOP.RequetePFMCOP;
import beansForJdbc.BeanBDAccess;
import protocole.ConsoleServeur;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.Security;
import java.sql.ResultSet;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 *
 * @author hector
 */
public class ServeurPFMCOP extends Thread {
    
    protected final int NB_THREADS = 3;
    
    protected int port;
    protected String madresse;
    protected int mport;
    protected SourceTaches tachesAExecuter;
    protected ConsoleServeur guiApplication;
    protected ServerSocket SSocket = null;
    protected MulticastSocket MSocket = null;

    public ServeurPFMCOP(int port, String madresse, int mport, ConsoleServeur guiApplication) {
        this.port = port;
        this.madresse = madresse;
        this.mport = mport;
        this.tachesAExecuter = new ListeTaches();
        this.guiApplication = guiApplication;
    }
    
    @Override
    public void run() {
        BeanBDAccess db = new BeanBDAccess("com.mysql.cj.jdbc.Driver", "jdbc:mysql://localhost:3306/bd_compta", "hector", "WA0UH.nice.key");
        try {
            db.creerConnexionBD();
            SSocket = new ServerSocket(port);
            //MSocket = new MulticastSocket(mport);
            //MSocket.joinGroup(InetAddress.getByName("228.5.6.7"));
            
            while(!isInterrupted()) {
                System.out.println("************ Serveur en attente");
                Socket CSocket = SSocket.accept();
                guiApplication.TraceEvenements(CSocket.getRemoteSocketAddress().toString() + "#accept#thread serveur");
                
                ObjectInputStream ois = new ObjectInputStream(CSocket.getInputStream());
                ObjectOutputStream oos = new ObjectOutputStream(CSocket.getOutputStream());
                RequetePFMCOP req = (RequetePFMCOP)ois.readObject();
                ReponsePFMCOP rep;
                
                System.out.println("Requete lue par le serveur, instance de " + req.getClass().getName());
                
                if(req.getType() == RequetePFMCOP.LOGIN_GROUP) {
                    String adresseDistante = CSocket.getRemoteSocketAddress().toString();
                    System.out.println("Début de Login_Group : adresse distante = " + adresseDistante);
                    // la charge utile est le nom et mot de passe
                    String cu = req.getChargeUtile();
                    
                    String[] parser = cu.split("  ");

                    if(parser.length >= 3) {
                        String user = parser[0];
                        String temps = parser[1];
                        String alea = parser[2];

                        try {
                            ResultSet rs = db.executeRequeteSelection("SELECT password FROM personnel WHERE login = '" + user + "'");
                            if(rs.next())
                            {
                                String pass = rs.getString("password");

                                // confection d'un digest local
                                Security.addProvider(new BouncyCastleProvider());
                                MessageDigest md = MessageDigest.getInstance("SHA-1", RequetePFMCOP.codeProvider);
                                md.update(user.getBytes());
                                md.update(pass.getBytes());
                                md.update(temps.getBytes());
                                md.update(alea.getBytes());

                                byte[] msgDLocal = md.digest();
                                
                                if(MessageDigest.isEqual(req.getDigest(), msgDLocal))
                                    //rep = new ReponsePFMCOP(ReponsePFMCOP.LOGIN_GROUP_OK, MSocket.getInetAddress() + "  " + MSocket.getPort());
                                    rep = new ReponsePFMCOP(ReponsePFMCOP.LOGIN_GROUP_OK, madresse + "  " + mport);
                                else
                                    rep = new ReponsePFMCOP(ReponsePFMCOP.WRONG_LOGIN, null);
                            }
                            else
                                rep = new ReponsePFMCOP(ReponsePFMCOP.WRONG_LOGIN, null);
                        } catch (Exception ex) {
                            rep = new ReponsePFMCOP(ReponsePFMCOP.SERVER_FAIL, null);
                        }
                    }
                    else
                        rep = new ReponsePFMCOP(ReponsePFMCOP.INVALID_FORMAT, null);
                }
                else
                    rep = new ReponsePFMCOP(ReponsePFMCOP.UNKNOWN_TYPE, null);
                
                System.out.println("rep = " + rep.getChargeUtile());
                oos.writeObject(rep);
                oos.flush();
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Erreur de def de classe [" + e.getMessage() + "]");
        } catch (IOException e) {
            System.err.println("Erreur IO ? [" + e.getMessage() + "]");
        } catch (Exception e) {
            System.err.println("Erreur de base de données ! ? [" + e.getMessage() + "]");
        }
    }
}
