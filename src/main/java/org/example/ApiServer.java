package org.example;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import org.example.endpoints.*;

import java.net.InetSocketAddress;

public class ApiServer {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        CorsFilter cors = new CorsFilter();

        // Health
        HttpContext health = server.createContext("/health", ex ->
                ApiUtils.sendJson(ex, 200, "{\"status\":\"ok\"}")
        );
        health.getFilters().add(cors);

        // Endpoints
        HttpContext usuarios = server.createContext("/usuarios", new UserHandler());
        usuarios.getFilters().add(cors);

        HttpContext login = server.createContext("/usuarios/login", new LoginHandler());
        login.getFilters().add(cors);

        HttpContext productos = server.createContext("/productos", new ProductHandler());
        productos.getFilters().add(cors);

        HttpContext procesos = server.createContext("/procesos", new ProcesosHandler());
        procesos.getFilters().add(cors);

        HttpContext root = server.createContext("/");
        root.getFilters().add(cors);
        root.setHandler(ex -> ApiUtils.sendJson(ex, 404, "{\"error\":\"Not Found\"}"));

        System.out.println("Servidor escuchando en http://localhost:" + port + "/");
        server.start();
    }
}
