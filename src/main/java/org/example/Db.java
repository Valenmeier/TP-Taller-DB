package org.example;

import java.sql.Connection;
import java.sql.DriverManager;

public class Db {
    private static final String URL = System.getenv("DB_URL");
    private static final String USER = System.getenv("DB_USER");
    private static final String PASS = System.getenv("DB_PASS");

    public static Connection getConnection() throws Exception {
        if (URL == null || USER == null || PASS == null) {
            throw new IllegalStateException(
                    "Faltan variables de entorno: DB_URL, DB_USER o DB_PASS"
            );
        }
        return DriverManager.getConnection(URL, USER, PASS);
    }


    public void conectarDB() {
        System.out.print("Conectando a MySQL... ");
        try (Connection conn = Db.getConnection()) {
            System.out.println("Conectado");
        } catch (Exception e) {
            System.out.println("Error al conectar");
            e.printStackTrace();
        }
    }
}
