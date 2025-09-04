package org.example.endpoints;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.dao.UsuarioDAO;
import org.example.model.Usuario;
import org.example.security.Jwt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class LoginHandler implements HttpHandler {
    private final Gson gson = ApiUtils.GSON;


    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            ApiUtils.sendJson(exchange, 405, "{\"error\":\"Método no permitido\"}");
            return;
        }
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Usuario creds = gson.fromJson(body, Usuario.class);

            UsuarioDAO dao = new UsuarioDAO();
            Usuario u = dao.findByUsuarioYContrasena(creds.getUsuario(), creds.getContrasena());

            if (u != null) {
                String token = Jwt.sign(u.getId(), u.getUsuario(), u.getRol());
                u.setContrasena(null);
                var resp = Map.of(
                        "token", token,
                        "user", Map.of("id", u.getId(), "usuario", u.getUsuario(), "rol", u.getRol())
                );
                ApiUtils.sendJson(exchange, 200, gson.toJson(resp));
            } else {
                ApiUtils.sendJson(exchange, 401, "{\"error\":\"Credenciales inválidas\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            ApiUtils.sendJson(exchange, 500, "{\"error\":\"Error interno\"}");
        }
    }
}
