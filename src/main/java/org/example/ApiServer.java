package org.example;

import com.sun.net.httpserver.HttpServer;
import org.example.endpoints.*;

import java.net.InetSocketAddress;

public class ApiServer {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Health check
        server.createContext("/health", exchange ->
                ApiUtils.sendJson(exchange, 200, "{\"status\":\"ok\"}")
        );

        // Endpoints separados en package
        server.createContext("/usuarios", new UserHandler());
        server.createContext("/usuarios/login", new LoginHandler());
        server.createContext("/productos", new ProductHandler());
        server.createContext("/procesos", new ProcesosHandler());

        System.out.println("Servidor escuchando en http://localhost:" + port + "/");
        server.start();
    }
}
