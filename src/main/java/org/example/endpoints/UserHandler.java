package org.example.endpoints;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.dao.UsuarioDAO;
import org.example.model.Usuario;
import org.example.security.Passwords;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class UserHandler implements HttpHandler {
    private final Gson gson = ApiUtils.GSON;
    private final UsuarioDAO dao = new UsuarioDAO();

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            DecodedJWT jwt = Auth.requireAuth(ex);
            Auth.requireRole(jwt, "ADMIN");

            switch (ex.getRequestMethod()) {
                case "GET" -> handleGet(ex);
                case "POST" -> handlePost(ex);
                case "DELETE" -> handleDelete(ex);
                case "PUT" -> handlePut(ex);
                default -> ApiUtils.sendJson(ex, 405, "{\"error\":\"Método no permitido\"}");
            }
        } catch (IllegalAccessException e) {
            ApiUtils.sendJson(ex, 401, "{\"error\":\"No autorizado\"}");
        } catch (Exception e) {
            e.printStackTrace();
            ApiUtils.sendJson(ex, 500, "{\"error\":\"Error interno\"}");
        }
    }

    // GET /usuarios
    private void handleGet(HttpExchange ex) throws Exception {
        List<Usuario> lista = dao.findAll();
        lista.forEach(u -> u.setContrasena(null));
        ApiUtils.sendJson(ex, 200, gson.toJson(lista));
    }

    // POST /usuarios  Body: { "usuario": "...", "contrasena": "...", "rol": "ENCARGADO|CAJERO|ADMIN" }
    private void handlePost(HttpExchange ex) throws Exception {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Usuario dto = gson.fromJson(body, Usuario.class);
        if (dto.getUsuario() == null || dto.getContrasena() == null || dto.getRol() == null) {
            ApiUtils.sendJson(ex, 400, "{\"error\":\"usuario, contrasena y rol son requeridos\"}");
            return;
        }
        Usuario creado = dao.create(new Usuario(null, dto.getUsuario(), dto.getContrasena(), dto.getRol()));
        creado.setContrasena(null);
        ApiUtils.sendJson(ex, 201, gson.toJson(creado));
    }

    // dentro de UsuariosHandler
    private void handlePut(HttpExchange ex) throws Exception {
        String path = ex.getRequestURI().getPath(); // ej: /usuarios/5
        String[] parts = path.split("/");
        if (parts.length < 3) {
            ApiUtils.sendJson(ex, 400, "{\"error\":\"Falta id\"}");
            return;
        }
        int id = Integer.parseInt(parts[2]);

        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Usuario dto = gson.fromJson(body, Usuario.class);

        Usuario existente = dao.findById(id);
        if (existente == null) {
            ApiUtils.sendJson(ex, 404, "{\"error\":\"No encontrado\"}");
            return;
        }

        // actualizar campos si vienen en el body
        if (dto.getUsuario() != null) existente.setUsuario(dto.getUsuario());
        if (dto.getRol() != null) existente.setRol(dto.getRol());
        if (dto.getContrasena() != null && !dto.getContrasena().isBlank()) {
            // si viene nueva contraseña → hash en updatePassword
            dao.updatePassword(id, dto.getContrasena());
        } else {
            dao.updatePerfil(existente);
        }

        existente.setContrasena(null);
        ApiUtils.sendJson(ex, 200, gson.toJson(existente));
    }


    // DELETE /usuarios/{id}
    private void handleDelete(HttpExchange ex) throws Exception {
        String path = ex.getRequestURI().getPath(); // ej: /usuarios/5
        String[] parts = path.split("/");
        if (parts.length < 3) {
            ApiUtils.sendJson(ex, 400, "{\"error\":\"Falta id\"}");
            return;
        }
        int id = Integer.parseInt(parts[2]);
        boolean ok = dao.delete(id);
        if (ok) ApiUtils.sendJson(ex, 200, "{\"ok\":true}");
        else    ApiUtils.sendJson(ex, 404, "{\"error\":\"No encontrado\"}");
    }
}
