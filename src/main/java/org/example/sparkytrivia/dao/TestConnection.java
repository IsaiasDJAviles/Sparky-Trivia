package org.example.sparkytrivia.dao;

import java.sql.Connection;
import java.sql.DriverManager;

public class TestConnection {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/sparkytrivia";
        String user = "sparky";
        String password = "sparky123";

        try {
            Class.forName("org.postgresql.Driver");
            Connection conn = DriverManager.getConnection(url, user, password);
            System.out.println("✅ Conexión exitosa a PostgreSQL!");
            conn.close();
        } catch (Exception e) {
            System.err.println("❌ Error de conexión: " + e.getMessage());
            e.printStackTrace();
        }
    }
}