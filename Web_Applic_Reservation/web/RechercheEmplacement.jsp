<%-- 
    Document   : RechercheEmplacement
    Created on : 8 Nov 2020, 15:28:44
    Author     : hector
--%>

<%@page import="java.text.SimpleDateFormat"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>

<%@page import="java.sql.*"%>
<%@page import="beansForJdbc.BeanBDAccess"%>

<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>JSP et JDBC</title>
    </head>
    <body>
        <h1>Résultat</h1>
        <%
            Object existe = session.getAttribute("logon.isDone");
            
            if(existe == null) {
                response.sendRedirect(request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + "/Web_Applic_Reservation/");
                return;
            }
            else {
                BeanBDAccess db = new BeanBDAccess("com.mysql.cj.jdbc.Driver", "jdbc:mysql://localhost:3306/bd_mouvements", "hector", "WA0UH.nice.key");
                try {
                    db.creerConnexionBD();
                }
                catch (Exception ex) {
                    return;
                }

                String[] emplacement = request.getParameter("emplacement").split(", ");
                String debut = request.getParameter("debut");
                String fin = request.getParameter("fin");
                
                java.util.Date dateDebut = new SimpleDateFormat("yyyy-MM-dd").parse(debut);
                java.util.Date dateFin = new SimpleDateFormat("yyyy-MM-dd").parse(fin);
                
                if(dateDebut.compareTo(dateFin) <= 0 && emplacement.length >= 2) {
                    ResultSet rs = db.executeRequeteSelection("SELECT * FROM occupations WHERE x = " + emplacement[0] + " AND y = " + emplacement[1] + " AND (dateDebut <= CAST('" + debut + "' AS DATE) AND (dateFin IS NULL OR dateFin >= CAST('" + fin + "' AS DATE)) OR dateDebut >= CAST('" + debut + "' AS DATE) AND dateDebut <= CAST('" + fin + "' AS DATE) OR dateFin >= CAST('" + debut + "' AS DATE) AND dateFin <= CAST('" + fin + "' AS DATE))");
                    if (rs.next()) {
        %>
        <p>Réservation impossible</p>
        <%          } else {
                        try {
                            db.executeRequeteMiseAJour("INSERT INTO occupations (container, x, y, dateDebut, dateFin) VALUES (" + request.getParameter("container") + ", " + emplacement[0] + ", " + emplacement[1] + ", CAST('" + debut + "' AS DATE), CAST('" + fin + "' AS DATE))");
                            rs = db.executeRequeteSelection("SELECT * FROM occupations WHERE container = " + request.getParameter("container") + " AND x = " + emplacement[0] + " AND y = " + emplacement[1] + " AND dateDebut = CAST('" + debut + "' AS DATE)");
                            
                            if(rs.next()) {
        %>
        <p>Numéro de réservation : <%=rs.getString("id") %></p>
        <%                  }
                        } catch (Exception ex) {
        %>
        <p>Erreur ! <%=ex.getMessage() %></p>
        <%
                        }
                    }
                }
            }
        %>
    </body>
</html>
