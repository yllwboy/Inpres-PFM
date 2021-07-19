<%-- 
    Document   : JSPInit
    Created on : 14 Nov 2020, 14:24:43
    Author     : hector
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>PFM Loisirs</title>
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
        <form method="GET" action="ShopServlet">
            <input type="radio" id="visites" name="choix" value="visites" checked />
            <label>visites au parc de loisirs verts et/ou à la réserve naturelle</label><br>
            <input type="radio" id="achats" name="choix" value="achats" />
            <label>achats de guides et objets "nature"</label><br><br>
            <input type="submit" name="continuer" value="Continuer" />
            <input type="submit" name="abandonner" value="Abandonner" />
        </form>
    </body>
</html>
