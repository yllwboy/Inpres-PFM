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

import ProtocoleCHAMAP.ReponseCHAMAP;
import ProtocoleCHAMAP.RequeteCHAMAP;
import ProtocolePLAMAP.RequetePLAMAP;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.Date;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import protocole.ConsoleServeur;

/**
 *
 * @author hector
 */
public class ServeurPLAMAP extends ThreadServeur {
    private String addr_compta;
    private int port_compta;
    
    ObjectOutputStream oos;
    ObjectInputStream ois;

    public ServeurPLAMAP(String addr_compta, int port_compta, int port, SourceTaches tachesAExecuter, ConsoleServeur guiApplication) {
        super(port, tachesAExecuter, guiApplication);
        this.addr_compta = addr_compta;
        this.port_compta = port_compta;
    }
    
    @Override
    public void run() {
        Socket socketClient;
        
        try {
            socketClient = new Socket(addr_compta, port_compta);
            System.out.println(socketClient.getInetAddress().toString());
        }
        catch (UnknownHostException e) {
            guiApplication.TraceEvenements("Erreur ! Host non trouvé [" + e + "]");
            return;
        }
        catch (IOException e) {
            guiApplication.TraceEvenements("Erreur ! Pas de connexion ? [" + e + "]");
            return;
        }
        
        String chargeUtile;
        String temps = Long.toString((new Date()).getTime());
        String alea = Double.toString(Math.random());
        byte[] msgD;

        try {
            String user = "john", password = "doe";

            System.out.println("Instanciation du message digest");
            Security.addProvider(new BouncyCastleProvider());
            MessageDigest md = MessageDigest.getInstance("SHA-1", RequeteCHAMAP.codeProvider);
            md.update(user.getBytes());
            md.update(password.getBytes());
            md.update(temps.getBytes());
            md.update(alea.getBytes());

            msgD = md.digest();
            chargeUtile = user + "  " + temps + "  " + alea;

        } catch (NoSuchAlgorithmException | NoSuchProviderException  ex) {
            guiApplication.TraceEvenements("Erreur ! [" + ex.getMessage() + "]");
            return;
        }

        // Envoi de la requête
        try {
            oos = new ObjectOutputStream(socketClient.getOutputStream());
            oos.writeObject(new RequeteCHAMAP(RequeteCHAMAP.LOGIN_TRAF, chargeUtile, msgD));
            oos.flush();
        }
        catch (IOException e) {
            guiApplication.TraceEvenements("Erreur réseau ? [" + e.getMessage() + "]");
            return;
        }

        // Lecture de la réponse
        ReponseCHAMAP rep = null;
        try {
            ois = new ObjectInputStream(socketClient.getInputStream());
            rep = (ReponseCHAMAP)ois.readObject();

            if(rep.getCode() == ReponseCHAMAP.LOGIN_TRAF_OK)
                guiApplication.TraceEvenements(" *** Reponse reçue : Connexion réussie");
            else if(rep.getCode() == ReponseCHAMAP.WRONG_LOGIN)
                guiApplication.TraceEvenements(" *** Reponse reçue : Nom d'utilisateur ou mot de passe erroné");
            else if(rep.getCode() == ReponseCHAMAP.ALREADY_LOGGED_IN)
                guiApplication.TraceEvenements(" *** Reponse reçue : Vous êtes déjà connecté");
            else if(rep.getCode() == ReponseCHAMAP.INVALID_FORMAT)
                guiApplication.TraceEvenements(" *** Reponse reçue : Le format de la commande est invalide");
            else if(rep.getCode() == ReponseCHAMAP.UNKNOWN_TYPE)
                guiApplication.TraceEvenements(" *** Reponse reçue : La commande est inconnue");
            else if(rep.getCode() == ReponseCHAMAP.SERVER_FAIL)
                guiApplication.TraceEvenements(" *** Reponse reçue : Erreur système du serveur");
            else
                guiApplication.TraceEvenements(" *** Reponse reçue : " + rep.getChargeUtile());

            if(rep.getCode() != ReponseCHAMAP.LOGIN_TRAF_OK) {
                socketClient.close();
                return;
            }
        }
        catch (ClassNotFoundException e) {
            guiApplication.TraceEvenements("--- erreur sur la classe = " + e.getMessage());
            return;
        }
        catch (IOException e) {
            guiApplication.TraceEvenements("--- erreur IO = " + e.getMessage());
            return;
        }
        
        try {
            SSocket = new ServerSocket(port);
        }
        catch (IOException e) {
            System.err.println("Erreur de port d'écoute ! ? [" + e + "]"); System.exit(1);
        }
        
// Démarrage du pool de threads
        for (int i = 0; i < NB_THREADS; i++) {
            ThreadClient thr = new ThreadClient(tachesAExecuter, "Thread du pool n°" + String.valueOf(i));
            thr.start();
        }
        
// Mise en attente du serveur
        Socket CSocket = null;
        
        while(!isInterrupted()) {
            try {
                System.out.println("************ Serveur en attente");
                CSocket = SSocket.accept();
                guiApplication.TraceEvenements(CSocket.getRemoteSocketAddress().toString() + "#accept#thread serveur");
                
                BufferedReader inFromServer = new BufferedReader(new InputStreamReader(CSocket.getInputStream()));
                String chu = inFromServer.readLine();
                System.out.println(chu);
                RequetePLAMAP req = new RequetePLAMAP(chu.split("\r\n")[0]);
                req.setIn(inFromServer);
                req.setCli_ois(ois);
                req.setCli_oos(oos);
                
                Runnable travail = req.createRunnable(CSocket, guiApplication);
                if (travail != null) {
                    tachesAExecuter.recordTache(travail);
                    System.out.println("Travail mis dans la file");
                }
                else
                    System.out.println("Pas de mise en file");
            }
            catch(IOException e) {
                System.err.println("Erreur d'accept ! ? [" + e.getMessage() + "]"); System.exit(1);
            }
        }
    }
}
