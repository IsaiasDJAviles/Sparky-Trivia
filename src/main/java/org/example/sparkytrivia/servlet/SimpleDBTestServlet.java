package org.example.sparkytrivia.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

@WebServlet(name = "SimpleDBTestServlet", urlPatterns = {"/dbtest"})
public class SimpleDBTestServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();

        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Test de Conexión PostgreSQL - Sparky Trivia</title>");
        out.println("<style>");
        out.println("body { font-family: 'Segoe UI', Arial, sans-serif; margin: 40px; background: #f5f5f5; }");
        out.println(".container { background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); max-width: 800px; margin: 0 auto; }");
        out.println("h1 { color: #333; border-bottom: 3px solid #4CAF50; padding-bottom: 10px; }");
        out.println(".success { color: #4CAF50; background: #e8f5e9; padding: 10px; border-radius: 5px; margin: 10px 0; }");
        out.println(".error { color: #f44336; background: #ffebee; padding: 10px; border-radius: 5px; margin: 10px 0; }");
        out.println(".info { background: #e3f2fd; padding: 10px; border-radius: 5px; margin: 10px 0; }");
        out.println("table { width: 100%; border-collapse: collapse; margin-top: 20px; }");
        out.println("th, td { padding: 12px; text-align: left; border-bottom: 1px solid #ddd; }");
        out.println("th { background-color: #4CAF50; color: white; }");
        out.println("tr:hover { background-color: #f5f5f5; }");
        out.println("</style>");
        out.println("</head>");
        out.println("<body>");
        out.println("<div class='container'>");
        out.println("<h1>🔍 Test de Conexión a PostgreSQL</h1>");

        String url = "jdbc:postgresql://localhost:5432/sparkytrivia";
        String user = "sparky";
        String password = "sparky123";

        try {
            // Cargar el driver
            Class.forName("org.postgresql.Driver");
            out.println("<div class='success'>✅ Driver PostgreSQL cargado correctamente</div>");

            // Conectar
            Connection conn = DriverManager.getConnection(url, user, password);
            out.println("<div class='success'>✅ Conexión exitosa a la base de datos 'sparkytrivia'</div>");

            // Información de la base de datos
            out.println("<div class='info'>");
            out.println("<strong>📊 Información de la conexión:</strong><br>");
            out.println("URL: " + url + "<br>");
            out.println("Usuario: " + user + "<br>");
            out.println("</div>");

            // Consultar las tablas
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT table_name FROM information_schema.tables " +
                            "WHERE table_schema = 'public' ORDER BY table_name"
            );

            out.println("<h2>📋 Tablas en la base de datos:</h2>");
            out.println("<table>");
            out.println("<tr><th>#</th><th>Nombre de la Tabla</th></tr>");

            int count = 0;
            while (rs.next()) {
                count++;
                String tableName = rs.getString("table_name");
                out.println("<tr><td>" + count + "</td><td>" + tableName + "</td></tr>");
            }

            out.println("</table>");
            out.println("<div class='info'><strong>Total de tablas:</strong> " + count + "</div>");

            // Contar usuarios
            rs = stmt.executeQuery("SELECT COUNT(*) as total FROM usuario");
            if (rs.next()) {
                int userCount = rs.getInt("total");
                out.println("<h2>👥 Usuarios en la Base de Datos</h2>");
                out.println("<div class='info'><strong>Total de usuarios registrados:</strong> " + userCount + "</div>");

                if (userCount > 0) {
                    // Mostrar usuarios
                    rs = stmt.executeQuery("SELECT usuarioid, nickname, email, firstname, lastname, rol FROM usuario LIMIT 10");
                    out.println("<table>");
                    out.println("<tr><th>ID</th><th>Nickname</th><th>Email</th><th>Nombre</th><th>Apellido</th><th>Rol</th></tr>");

                    while (rs.next()) {
                        out.println("<tr>");
                        out.println("<td>" + rs.getInt("usuarioid") + "</td>");
                        out.println("<td>" + rs.getString("nickname") + "</td>");
                        out.println("<td>" + rs.getString("email") + "</td>");
                        out.println("<td>" + rs.getString("firstname") + "</td>");
                        out.println("<td>" + rs.getString("lastname") + "</td>");
                        out.println("<td>" + rs.getString("rol") + "</td>");
                        out.println("</tr>");
                    }
                    out.println("</table>");
                }
            }

            // Cerrar conexiones
            rs.close();
            stmt.close();
            conn.close();

            out.println("<div class='success'>");
            out.println("<h3>✅ ¡Todo funcionando correctamente!</h3>");
            out.println("<p>La conexión a PostgreSQL está operativa y lista para usar.</p>");
            out.println("</div>");

        } catch (ClassNotFoundException e) {
            out.println("<div class='error'>");
            out.println("<h3>❌ Error: Driver PostgreSQL no encontrado</h3>");
            out.println("<p>Asegúrate de tener la dependencia en pom.xml:</p>");
            out.println("<pre>&lt;dependency&gt;\n");
            out.println("    &lt;groupId&gt;org.postgresql&lt;/groupId&gt;\n");
            out.println("    &lt;artifactId&gt;postgresql&lt;/artifactId&gt;\n");
            out.println("    &lt;version&gt;42.7.1&lt;/version&gt;\n");
            out.println("&lt;/dependency&gt;</pre>");
            out.println("<p><strong>Error:</strong> " + e.getMessage() + "</p>");
            out.println("</div>");
        } catch (Exception e) {
            out.println("<div class='error'>");
            out.println("<h3>❌ Error de conexión</h3>");
            out.println("<p><strong>Mensaje:</strong> " + e.getMessage() + "</p>");
            out.println("<h4>Posibles causas:</h4>");
            out.println("<ul>");
            out.println("<li>PostgreSQL no está corriendo en el puerto 5432</li>");
            out.println("<li>La base de datos 'sparkytrivia' no existe</li>");
            out.println("<li>Usuario 'sparky' o contraseña incorrectos</li>");
            out.println("<li>PostgreSQL no está configurado para aceptar conexiones locales</li>");
            out.println("</ul>");
            out.println("<h4>Soluciones:</h4>");
            out.println("<ol>");
            out.println("<li>Verifica que PostgreSQL esté corriendo: <code>Get-Service postgresql*</code></li>");
            out.println("<li>Conéctate manualmente: <code>psql -U sparky -d sparkytrivia</code></li>");
            out.println("<li>Si la BD no existe, créala desde psql</li>");
            out.println("</ol>");
            out.println("</div>");
        }

        out.println("</div>");
        out.println("</body>");
        out.println("</html>");
    }
}