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
package beansForJdbc;

import java.beans.*;
import java.sql.*;

/**
 *
 * @author hector
 */
public class BeanBDAccess extends Object implements java.io.Serializable {
    /** Variable membre de la propriété DataBase.
    */
    public String dataBase;
    /** Variable membre de la propriété PiloteJdbc.
    */
    public String driverJdbc;
    /** Variable membre.
    * Représente la connexion en cours.
    */
    private Connection con;
    /** Variable membre.
    * Représente l'instruction en cours.
    */
    private java.sql.Statement instruc;
    /** Holds value of property user. */
    private String user;
    /** Holds value of property password. */
    private String password;
    /** Holds value of property requete. */
    private String requete;
    
    /** Creates new beanAccess */
    public BeanBDAccess() {
        driverJdbc = "sun.jdbc.odbc.JdbcOdbcDriver";
        user = "";
        password = "";
    }
    
    /** Creates new beanAccess.
    * @param db New value of property dataBase.
    * @param dv New value of property piloteJdbc.
    */
    public BeanBDAccess(String dv, String db) {
        driverJdbc = dv;
        dataBase = db;
        user = "";
        password = "";
    }
    
    /** Creates new beanAccess.
    * @param db New value of property dataBase.
    * @param dv New value of property piloteJdbc.
    * @param u New value of property user.
    * @param p New value of property password.
    */
    public BeanBDAccess(String dv, String db, String u, String p) {
        driverJdbc = dv;
        dataBase = db;
        user = u;
        password = p;
    }
    
    /** Getter for property dataBase.
    * @return Value of property dataBase.
    */
    public String getDataBase() { 
        return dataBase;
    }
    
    /** Setter for property dataBase.
    * @param dataBase New value of property dataBase.
    */
    public void setDataBase(String dataBase) {
        this.dataBase = dataBase;
    }
    
    /** Getter for property piloteJdbc.
    * @return Value of property piloteJdbc.
    */
    public String getPiloteJdbc() { 
        return driverJdbc;
    }
    
    /** Setter for property piloteJdbc.
    * @param piloteJdbc New value of property piloteJdbc.
    */
    public void setPiloteJdbc(String piloteJdbc) {
        driverJdbc = piloteJdbc;
    }
    
    /** Getter for property password.
    * @return Value of property password.
    */
    public String getPassword() { 
        return password;
    }
    
    /** Setter for property password.
    * @param password New value of property password.
    */
    public void setPassword(String password) {
        this.password = password;
    }
    
    /** Getter for property user.
    * @return Value of property user.
    */
    public String getUser() {
        return user;
    }
    
    /** Setter for property user.
    * @param user New value of property user.
    */
    public void setUser(String user) {
        this.user = user;
    }
    
    /** Getter for property requete.
    * @return Value of property requete.
    */
    public String getRequete() {
        return requete;
    }
    
    /** Setter for property requete.
    * @param requete New value of property requete.
    */
    public void setRequete(String r) {
        requete = r;
    }
    
    /** Méthode de connexion à une base de données.
    */
    public void creerConnexionBD() throws Exception {
        Class.forName(getPiloteJdbc());
        con = DriverManager.getConnection(getDataBase(), getUser(), getPassword());
        instruc = con.createStatement();
    }
    
    /** Méthode d'exécution d'une requête de sélection.
    * La requête sous forme de chaîne de caractères est la variable membre
    */
    synchronized public ResultSet executeRequeteSelection() throws Exception {
        return instruc.executeQuery(getRequete()); // synchronized() needed ???
    }
    
    /** Méthode d'exécution d'une requête de sélection.
    * @param r La requête sous forme de chaîne de caractères
    */
    synchronized public ResultSet executeRequeteSelection(String r) throws Exception {
        return instruc.executeQuery(r);
    }
    
    synchronized public void executeRequeteMiseAJour(String r) throws Exception {
        instruc.executeUpdate(r);
    }
}
