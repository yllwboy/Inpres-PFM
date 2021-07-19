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
            out.println("<title>Login PFM Loisirs</title>");            
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>PFM Loisirs ! Le site des loisirs verts près de chez vous</h1>");
            HttpSession session = request.getSession(true);
            Object existe = session.getAttribute("session.identificateur");
            if(existe != null) {
                response.sendRedirect(request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + "/Web_Applic_Loisirs/JSPInit.jsp");
                return;
            }
            ResultSet rs = db.executeRequeteSelection("SELECT pass FROM users WHERE name = '" + request.getParameter("name") + "'");
            if(request.getParameter("entrer") != null) {
                if(rs.next() && request.getParameter("pass").equals(rs.getString("pass"))) {
                    session.setAttribute("session.identificateur", request.getParameter("name"));
                    response.sendRedirect(request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + "/Web_Applic_Loisirs/JSPInit.jsp");
                    return;
                }
                else
                    out.println("<p>Nom d'utilisateur ou mot de passe incorrect !</p>");
            }
            else {
                if(request.getParameter("creer") != null) {
                    out.println("<form method=\"POST\" action=\"LoginServlet\">");
                    out.println("<label>Votre identifiant :</label>");
                    out.println("<input type=\"text\" id=\"name\" name=\"name\" value=\"" + request.getParameter("name") + "\" /><br>");
                    out.println("<label>Choisissez un mot de passe :</label>");
                    out.println("<input type=\"password\" id=\"pass\" name=\"pass\" /><br><br>");
                    out.println("<label>Confirmer votre mot de passe :</label>");
                    out.println("<input type=\"password\" id=\"conf\" name=\"conf\" /><br><br>");
                    out.println("<input type=\"submit\" name=\"confirmer\" value=\"Confirmer\" /><br>");
                    out.println("<input type=\"submit\" name=\"abandonner\"  value=\"Abandonner\" />");
                    out.println("</form>");
                }
                else {
                    if(request.getParameter("confirmer") != null) {
                        String pass = request.getParameter("pass");
                        if(pass.equals(request.getParameter("conf"))) {
                            db.executeRequeteMiseAJour("INSERT INTO users (name, pass) VALUES ('" + request.getParameter("name") + "', '" + request.getParameter("pass") + "')");
                            out.println("<form method=\"POST\" action=\"LoginServlet\">");
                            out.println("<label>Votre identifiant :</label>");
                            out.println("<input type=\"text\" id=\"name\" name=\"name\" value=\"" + request.getParameter("name") + "\" /><br>");
                            out.println("<label>Votre mot de passe :</label>");
                            out.println("<input type=\"password\" id=\"pass\" name=\"pass\" value=\"" + request.getParameter("pass") + "\" /><br><br>");
                            out.println("<input type=\"submit\" name=\"entrer\" value=\"Entrer sur le site\" /><br>");
                            out.println("<input type=\"submit\" name=\"creer\"  value=\"Je n'ai pas de mot de passe\" />");
                            out.println("</form>");
                        }
                        else {
                            out.println("<form method=\"POST\" action=\"LoginServlet\">");
                            out.println("<label>Votre identifiant :</label>");
                            out.println("<input type=\"text\" id=\"name\" name=\"name\" value=\"" + request.getParameter("name") + "\" /><br>");
                            out.println("<label>Choisissez un mot de passe :</label>");
                            out.println("<input type=\"password\" id=\"pass\" name=\"pass\" /><br><br>");
                            out.println("<label>Confirmer votre mot de passe :</label>");
                            out.println("<input type=\"password\" id=\"conf\" name=\"conf\" />");
                            out.println("<p>Le mot de passe et sa confirmation ne correspondent pas</p><br>");
                            out.println("<input type=\"submit\" name=\"confirmer\" value=\"Confirmer\" /><br>");
                            out.println("<input type=\"submit\" name=\"abandonner\"  value=\"Abandonner\" />");
                            out.println("</form>");
                        }
                    }
                    else {
                        response.sendRedirect(request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + "/Web_Applic_Loisirs/");
                        return;
                    }
                }
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
