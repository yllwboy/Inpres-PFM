<%-- 
    Document   : JSPCaddie
    Created on : 14 Nov 2020, 14:25:01
    Author     : hector
--%>

<%@page import="java.math.BigDecimal"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>

<%@page import="java.sql.*"%>
<%@page import="beansForJdbc.BeanBDAccess"%>

<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Mon caddie</title>
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
            
            String delete = request.getParameter("deltic");
            String date = request.getParameter("date");
            if(delete != null)
                db.executeRequeteMiseAJour("DELETE FROM users_tickets WHERE user = " + id + " AND ticket = '" + delete + "' AND date = CAST('" + date + "' AS DATE)");
            
            delete = request.getParameter("delpro");
            if(delete != null)
                db.executeRequeteMiseAJour("DELETE FROM users_products WHERE user = " + id + " AND product = '" + delete + "'");
            
            String ticket = request.getParameter("ticket");
            if(ticket != null) {
                Boolean add = false;
                int quantite = Integer.parseInt(request.getParameter("quantite"));
                rs = db.executeRequeteSelection("SELECT id, nom, prix, date, places - SUM(quantite) AS 'places' FROM tickets LEFT JOIN users_tickets ON id = ticket WHERE places IS NOT NULL AND id = '" + ticket + "' AND date = CAST('" + date + "' AS DATE) GROUP BY id, date");
                if(rs.next()) {
                    int places = Integer.parseInt(rs.getString("places"));
                    if(places < quantite) {
                    %>
            <h2>Il n'y a pas suffisamment de places pour le jour sélectionné !</h2>
                    <%
                    }
                    else
                        add = true;
                }
                else
                    add = true;
                if(add) {
                    rs = db.executeRequeteSelection("SELECT * FROM users_tickets WHERE user = " + id + " AND ticket = '" + ticket + "' AND date = CAST('" + date + "' AS DATE)");
                    if(rs.next())
                        db.executeRequeteMiseAJour("UPDATE users_tickets SET quantite = quantite + " + quantite + ", dateAjout = CURRENT_TIMESTAMP() WHERE user = " + id + " AND product = '" + ticket + "' AND date = CAST('" + date + "' AS DATE)");
                    else
                        db.executeRequeteMiseAJour("INSERT INTO users_tickets VALUES (" + id + ", '" + ticket + "', CAST('" + date + "' AS DATE), " + quantite + ", CURRENT_TIMESTAMP())");
                }
            }
            
            String product = request.getParameter("product");
            if(product != null) {
                int quantite = Integer.parseInt(request.getParameter("quantite"));
                rs = db.executeRequeteSelection("SELECT id, nom, prix, stock, stock - SUM(quantite) AS 'stockleft' FROM products LEFT JOIN users_products ON id = product WHERE id = '" + product + "' GROUP BY id");
                if(rs.next()) {
                    int stock = 0;
                    if(rs.getString("stockleft") != null)
                        stock = Integer.parseInt(rs.getString("stockleft"));
                    else
                        stock = Integer.parseInt(rs.getString("stock"));
                    if(stock < quantite) {
                    %>
            <h2>Le produit n'est plus disponible en stock en exemplaires suffisant !</h2>
                    <%
                    }
                    else {
                        rs = db.executeRequeteSelection("SELECT * FROM users_products WHERE user = " + id + " AND product = '" + product + "'");
                        if(rs.next())
                            db.executeRequeteMiseAJour("UPDATE users_products SET quantite = quantite + " + quantite + ", dateAjout = CURRENT_TIMESTAMP() WHERE user = " + id + " AND product = '" + product + "'");
                        else
                            db.executeRequeteMiseAJour("INSERT INTO users_products VALUES (" + id + ", '" + product + "', " + quantite + ", CURRENT_TIMESTAMP())");
                    }
                }
                
            }

            rs = db.executeRequeteSelection("SELECT * FROM tickets INNER JOIN users_tickets ON id = ticket WHERE user = " + id);

            BigDecimal prix;
            BigDecimal total = new BigDecimal("0");
            while(rs.next()) {
                prix = new BigDecimal(rs.getString("quantite"));
                prix = prix.multiply(new BigDecimal(rs.getString("prix")));
                total = total.add(prix);
        %>
        <form method="POST" action="JSPCaddie.jsp">
            <p><%=rs.getString("quantite") %> x <%=rs.getString("nom") %> : <%=rs.getString("date") %></p>
            <p>€<%=prix.toString() %></p>
            <input type="hidden" value="<%=rs.getString("id") %>" id="deltic" name="deltic" />
            <input type="hidden" value="<%=rs.getString("date") %>" id="date" name="date" />
            <input type="submit" value="Enlèver du panier" />
        </form>
            <hr>
        <%
            }

            rs = db.executeRequeteSelection("SELECT * FROM products INNER JOIN users_products ON id = product WHERE user = " + id);

            while(rs.next()) {
                prix = new BigDecimal(rs.getString("quantite"));
                prix = prix.multiply(new BigDecimal(rs.getString("prix")));
                total = total.add(prix);
        %>
        <form method="POST" action="JSPCaddie.jsp">
            <p><%=rs.getString("quantite") %> x <%=rs.getString("nom") %></p>
            <p>€<%=prix.toString() %></p>
            <input type="hidden" value="<%=rs.getString("id") %>" id="delpro" name="delpro" />
            <input type="submit" value="Enlèver du panier" />
        </form>
            <hr>
        <%
            }
        %><h2>Total : €<%=total.toString() %></h2>
        <a href="JSPPay.jsp">Payer</a>
        <hr>
        <a href="JSPInit.jsp">Retour sur la page principale</a>
    </body>
</html>
