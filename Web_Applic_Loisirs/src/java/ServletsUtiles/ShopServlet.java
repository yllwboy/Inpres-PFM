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
package ServletsUtiles;

import beansForJdbc.BeanBDAccess;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * @author hector
 */
public class ShopServlet extends HttpServlet {
    private BeanBDAccess db;
    
    @Override
    public void init() throws ServletException {
        super.init();
        db = new BeanBDAccess("com.mysql.cj.jdbc.Driver", "jdbc:mysql://localhost:3306/bd_shopping", "hector", "WA0UH.nice.key");
        try {
            db.creerConnexionBD();
        }
        catch (Exception ex) {
            throw new ServletException("Database error: " + ex.getMessage());
        }
    }
    
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        try ( PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>PFM Loisirs</title>");
            out.println("</head>");
            out.println("<body>");
            HttpSession session = request.getSession(true);
            if(request.getParameter("abandonner") != null) {
                session.invalidate();
                response.sendRedirect(request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + "/Web_Applic_Loisirs/");
                return;
            }
            Object existe = session.getAttribute("session.identificateur");
            if(existe == null) {
                response.sendRedirect(request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + "/Web_Applic_Loisirs/");
                return;
            }
            out.println("<h1>PFM Loisirs ! Le site des loisirs verts près de chez vous - Session " + existe + "</h1>");
            
            try {
                Boolean choix = "visites".equals(request.getParameter("choix"));
                ResultSet rs;
                if(choix)
                    rs = db.executeRequeteSelection("SELECT * FROM tickets");
                else
                    rs = db.executeRequeteSelection("SELECT id, nom, prix, stock, stock - SUM(quantite) AS 'stockleft' FROM products LEFT JOIN users_products ON id = product GROUP BY id");
                while(rs.next()) {
                    out.println("<form method=\"POST\" action=\"JSPCaddie.jsp\">");
                    out.println("<p>" + rs.getString("nom") + " - €" + rs.getString("prix") + "</p>");
                    int stock = 0;
                    if(choix) {
                        String places = rs.getString("places");
                        if(places != null)
                            stock = Integer.parseInt(places);
                    }
                    else {
                        if(rs.getString("stockleft") != null)
                            stock = Integer.parseInt(rs.getString("stockleft"));
                        else
                            stock = Integer.parseInt(rs.getString("stock"));
                    }
                    if(choix) {
                        if(stock > 0) {
                            out.println("<p>Places disponibles par jour : " + stock + "</p>");
                            out.println("<label>Quantité :</label>");
                            out.println("<input type=\"number\" value=\"1\" min=\"1\" max=\"" + stock + "\" id=\"quantite\" name=\"quantite\" required />");
                        }
                        else {
                            out.println("<label>Quantité :</label>");
                            out.println("<input type=\"number\" value=\"1\" min=\"1\" id=\"quantite\" name=\"quantite\" required />");
                        }
                        out.println("<label>Date :</label>");
                        out.println("<input type=\"date\" id=\"date\" name=\"date\" required />");
                        out.println("<input type=\"hidden\" value=\"" + rs.getString("id") + "\" id=\"ticket\" name=\"ticket\" />");
                        out.println("<input type=\"submit\" value=\"Ajouter au panier\" />");
                    }
                    else {
                        if(stock > 0) {
                            out.println("<p>Stock : " + stock + "</p>");
                            out.println("<label>Quantité :</label>");
                            out.println("<input type=\"number\" value=\"1\" min=\"1\" max=\"" + stock + "\" id=\"quantite\" name=\"quantite\" required />");
                            out.println("<input type=\"hidden\" value=\"" + rs.getString("id") + "\" id=\"product\" name=\"product\" />");
                            out.println("<input type=\"submit\" value=\"Ajouter au panier\" />");
                        }
                        else
                            out.println("<p>Rupture de stock</p>");
                    }
                    out.println("</form>");
                    out.println("<hr>");
                }
            } catch (SQLException ex) {
                out.println("<p>Erreur SQL ! " + ex.getMessage() + "</p>");
            } catch (Exception ex) {
                out.println("<p>Erreur ! " + ex.getMessage() + "</p>");
            }
            out.println("<a href=\"JSPCaddie.jsp\">Voir mon caddie</a>");
            out.println("</body>");
            out.println("</html>");
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
