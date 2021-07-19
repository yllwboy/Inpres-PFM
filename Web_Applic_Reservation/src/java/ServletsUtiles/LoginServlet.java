/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ServletsUtiles;

import beansForJdbc.BeanBDAccess;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * @author hector
 */
public class LoginServlet extends HttpServlet {
    
    private BeanBDAccess db;
    
    @Override
    public void init() throws ServletException {
        super.init();
        db = new BeanBDAccess("com.mysql.cj.jdbc.Driver", "jdbc:mysql://localhost:3306/bd_mouvements", "hector", "WA0UH.nice.key");
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
            out.println("<title>Réservation</title>");            
            out.println("</head>");
            out.println("<body>");
            
            HttpSession session = request.getSession(true);
            Object existe = session.getAttribute("logon.isDone");
            
            ResultSet rs = db.executeRequeteSelection("SELECT pass FROM users WHERE name = '" + request.getParameter("name") + "'");
            if(existe != null || (rs.next() && request.getParameter("pass").equals(rs.getString("pass")))) {
                if(existe == null)
                    session.setAttribute("logon.isDone", request.getParameter("name"));

                out.println("<h1>Bonjour " + session.getAttribute("logon.isDone") + " !</h1>");
                out.println("<form method=\"POST\" action=\"RechercheEmplacement.jsp\">");
                out.println("<label>Container :</label>");
                out.println("<select id=\"container\" name=\"container\">");
                
                rs = db.executeRequeteSelection("SELECT * FROM containers");
                while(rs.next())
                    out.println("<option value=\"" + rs.getString("id") + "\">" + rs.getString("id") + "</option>");
                out.println("</select><br>");
                out.println("<label>Emplacement :</label>");
                out.println("<select id=\"emplacement\" name=\"emplacement\">");
                
                rs = db.executeRequeteSelection("SELECT * FROM parc");
                while(rs.next())
                    out.println("<option value=\"" + rs.getString("x") + ", " + rs.getString("y") + "\">" + rs.getString("x") + ", " + rs.getString("y") + "</option>");
                out.println("</select><br>");
                out.println("<label>Date de début :</label>");
                out.println("<input type=\"date\" id=\"debut\" name=\"debut\" required /><br>");
                out.println("<label>Date de fin :</label>");
                out.println("<input type=\"date\" id=\"fin\" name=\"fin\" required /><br>");
                out.println("<input type=\"submit\" value=\"Réserver\" />");
                out.println("</form>");
            }
            else {
                out.println("<h1>Erreur !</h1>");
                out.println("<h2>Nom d'utilisateur ou mot de passe incorrect !</h2>");
            }
            out.println("</body>");
            out.println("</html>");
        } catch (Exception ex) {
            throw new ServletException("Database error: " + ex.getMessage());
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
