/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ProtocolePFMCOP;

import java.io.*;
import java.net.*;
import protocole.ConsoleServeur;
import protocole.Requete;

/**
 *
 * @author hector
 */
public class RequetePFMCOP implements Requete, Serializable {
    private static final long serialVersionUID = 6279354070353143569L;
    
    public static String codeProvider = "BC"; //CryptixCrypto";
    
    public static int LOGIN_GROUP = 1;
    public static int POST_QUESTION = 2;
    public static int ANSWER_QUESTION = 3;
    public static int POST_EVENT = 4;
    
    private int type;
    private String chargeUtile;
    private byte[] digest;

    public RequetePFMCOP(int t, String chu) {
        type = t;
        chargeUtile = chu;
    }
    
    public RequetePFMCOP(int t, String chu, byte[] dig) {
        type = t;
        chargeUtile = chu;
        digest = dig;
    }

    public String getChargeUtile() {
        return chargeUtile;
    }
    
    public byte[] getDigest() {
        return digest;
    }

    public int getType() {
        return type;
    }
    
    @Override
    public Runnable createRunnable(Socket s, ConsoleServeur cs) {
        return new Runnable() {
            public void run() {

            }
        };
    }
}
