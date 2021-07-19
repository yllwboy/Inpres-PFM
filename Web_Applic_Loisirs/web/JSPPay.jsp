<%-- 
    Document   : JSPPay
    Created on : 14 Nov 2020, 16:23:07
    Author     : hector
--%>

<%@page import="java.math.BigDecimal"%>
<%@page import="java.sql.ResultSet"%>
<%@page import="beansForJdbc.BeanBDAccess"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Paiement</title>
    </head>
    <body>
        <% 
            Object existe = session.getAttribute("session.identificateur");
            if(existe == null) {
                response.sendRedirect(request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + "/Web_Applic_Loisirs/");
                return;
            }
        %>
        <h1>PFM Loisirs ! Le site des loisirs verts près de chez vous - Session <%=existe %></h1>
        <%
            if(request.getParameter("abandonner") != null) {
                response.sendRedirect(request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + "/Web_Applic_Loisirs/JSPCaddie.jsp");
                return;
            }
            
            BeanBDAccess db = new BeanBDAccess("com.mysql.cj.jdbc.Driver", "jdbc:mysql://localhost:3306/bd_shopping", "hector", "WA0UH.nice.key");
            try {
                db.creerConnexionBD();
            }
            catch (Exception ex) {
                return;
            }

            ResultSet rs = db.executeRequeteSelection("SELECT id FROM users WHERE name = '" + session.getAttribute("session.identificateur") + "'");
            String id = "";
            if(rs.next())
                id = rs.getString("id");
            
            Boolean caddieVide = true;
            rs = db.executeRequeteSelection("SELECT * FROM users_tickets WHERE user = " + id);
            if(rs.next())
                caddieVide = false;
            rs = db.executeRequeteSelection("SELECT * FROM users_products WHERE user = " + id);
            if(rs.next())
                caddieVide = false;
            
            if(caddieVide) {
        %>
        <p>Le caddie est vide, ajoutez des produits d'abord !</p>
        <a href="JSPInit.jsp">Retour à la page principale</a>
    </body>
</html>
        <%
                return;
            }
            
            String operation = request.getParameter("operation");
            if(operation == null) {
        %>
        <form method="POST" action="JSPPay.jsp">
            <input type="radio" id="fin" name="operation" checked />
            <label>fin de session et paiement</label><br>
            <input type="submit" name="continuer" value="Continuer" />
            <input type="submit" name="abandonner" value="Abandonner" />
        </form>
        <%
            }
            else {
        %>
        <form method="POST" action="JSPPay.jsp">
            <label>Numéro de la carte de crédit :</label>
            <input type="text" id="credit" name="credit" required /><br>
            <label>Date de validité :</label>
            <input type="month" id="date" name="date" required /><br>
            <label>Votre code secret :</label>
            <input type="password" id="pass" name="pass" required /><br><br>
            <input type="submit" name="payer" value="Payer" />
        </form>
        <form method="POST" action="JSPPay.jsp">
            <input type="submit" name="abandonner" value="Abandonner" />
        </form>
        <%
            }
            
            String credit = request.getParameter("credit");
            if(credit != null) {
                BeanBDAccess dbw = new BeanBDAccess("com.mysql.cj.jdbc.Driver", "jdbc:mysql://localhost:3306/bd_shopping", "hector", "WA0UH.nice.key");
                try {
                    dbw.creerConnexionBD();
                }
                catch (Exception ex) {
                    return;
                }

                rs = db.executeRequeteSelection("SELECT * FROM users_tickets INNER JOIN tickets ON ticket = id WHERE user = " + id);
                while(rs.next()) {
                    BigDecimal prix = new BigDecimal(rs.getString("quantite"));
                    prix = prix.multiply(new BigDecimal(rs.getString("prix")));
                    dbw.executeRequeteMiseAJour("INSERT INTO purchases (user, ticket, quantite, paiement, date) VALUES (" + id + ", '" + rs.getString("id") + "', " + rs.getString("quantite") + ", " + prix.toString() + ", CURRENT_TIMESTAMP())");
                }

                db.executeRequeteMiseAJour("DELETE FROM users_tickets WHERE user = " + id);
                
                rs = db.executeRequeteSelection("SELECT * FROM users_products INNER JOIN products ON product = id WHERE user = " + id);
                while(rs.next()) {
                    BigDecimal prix = new BigDecimal(rs.getString("quantite"));
                    prix = prix.multiply(new BigDecimal(rs.getString("prix")));
                    dbw.executeRequeteMiseAJour("UPDATE products SET stock = stock - " + rs.getString("quantite") + " WHERE id = '" + rs.getString("id") + "'");
                    dbw.executeRequeteMiseAJour("INSERT INTO purchases (user, product, quantite, paiement, date) VALUES (" + id + ", '" + rs.getString("id") + "', " + rs.getString("quantite") + ", " + prix.toString() + ", CURRENT_TIMESTAMP())");
                }
                
                db.executeRequeteMiseAJour("DELETE FROM users_products WHERE user = " + id);
        %>
        <h2>Paiement effectué !</h2>
        <a href="JSPInit.jsp">Retour à la page principale</a>
        <%
            }
        %>
    </body>
</html>
