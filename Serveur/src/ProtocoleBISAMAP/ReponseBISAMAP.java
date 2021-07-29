/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ProtocoleBISAMAP;

import java.io.Serializable;
import protocole.Reponse;

/**
 *
 * @author hector
 */
public class ReponseBISAMAP implements Reponse, Serializable {
    private static final long serialVersionUID = 6279354070353143569L;
    
    public static int LOGIN_OK = 100;
    public static int WRONG_LOGIN = 101;
    public static int ALREADY_LOGGED_IN = 102;
    public static int GET_NEXT_BILL_OK = 200;
    public static int VALIDATE_BILL_OK = 300;
    public static int LIST_BILLS_OK = 400;
    public static int SEND_BILLS_OK = 500;
    public static int REC_PAY_OK = 600;
    public static int LIST_WAITING_OK = 700;
    public static int NOT_LOGGED_IN = 997;
    public static int INVALID_FORMAT = 998;
    public static int UNKNOWN_TYPE = 999;
    public static int SERVER_FAIL = -1;
    
    private int codeRetour;
    private String chargeUtile;
    private byte[] donneesCryptees;
    
    public ReponseBISAMAP(int c, String chu) {
        codeRetour = c;
        setChargeUtile(chu);
    }

    public ReponseBISAMAP(int codeRetour, String chargeUtile, byte[] donneesCryptees) {
        this.codeRetour = codeRetour;
        this.chargeUtile = chargeUtile;
        this.donneesCryptees = donneesCryptees;
    }
    
    @Override
    public int getCode() {
        return codeRetour;
    }
    
    public String getChargeUtile() {
        return chargeUtile;
    }

    public void setChargeUtile(String chargeUtile) {
        this.chargeUtile = chargeUtile;
    }

    public byte[] getDonneesCryptees() {
        return donneesCryptees;
    }

    public void setDonneesCryptees(byte[] donneesCryptees) {
        this.donneesCryptees = donneesCryptees;
    }
    
    
}
