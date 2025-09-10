package org.example.endpoints;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.dao.UsuarioDAO;
import org.example.model.Usuario;

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

    // POST
    private void handlePost(HttpExchange ex) throws Exception {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Usuario dto = gson.fromJson(body, Usuario.class);
        String usuario = normUser(dto.getUsuario());
        String pass = dto.getContrasena();
        String rol = dto.getRol();
        if (usuario == null || usuario.isBlank() || pass == null || pass.isBlank() || rol == null || rol.isBlank()) {
            ApiUtils.sendJson(ex, 400, "{\"error\":\"usuario, contrasena y rol son requeridos\"}");
            return;
        }
        try {
            Usuario creado = dao.create(new Usuario(null, usuario, pass, rol));
            creado.setContrasena(null);
            ApiUtils.sendJson(ex, 201, gson.toJson(creado));
        } catch (IllegalArgumentException dup) {
            ApiUtils.sendJson(ex, 409, "{\"error\":\"Usuario duplicado\"}");
        }
    }

    private void handlePut(HttpExchange ex) throws Exception {
        String path = ex.getRequestURI().getPath();
        String[] parts = path.split("/");
        if (parts.length < 3) {
            ApiUtils.sendJson(ex, 400, "{\"error\":\"Falta id\"}");
            return;
        }
        int id = Integer.parseInt(parts[2]);

        Usuario existente = dao.findById(id);
        if (existente == null) {
            ApiUtils.sendJson(ex, 404, "{\"error\":\"No encontrado\"}");
            return;
        }

        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Usuario dto = gson.fromJson(body, Usuario.class);

        boolean tocóPerfil = false;
        if (dto.getUsuario()!=null && !dto.getUsuario().isBlank()) {
            existente.setUsuario(normUser(dto.getUsuario()));
            tocóPerfil = true;
        }
        if (dto.getRol()!=null && !dto.getRol().isBlank()) {
            existente.setRol(dto.getRol());
            tocóPerfil = true;
        }

        try {
            if (tocóPerfil) {
                boolean ok = dao.updatePerfil(existente);
                if (!ok) {
                    ApiUtils.sendJson(ex, 500, "{\"error\":\"No se pudo actualizar perfil\"}");
                    return;
                }
            }

            if (dto.getContrasena()!=null && !dto.getContrasena().isBlank()) {
                boolean okPwd = dao.updatePassword(id, dto.getContrasena());
                if (!okPwd) {
                    ApiUtils.sendJson(ex, 500, "{\"error\":\"No se pudo actualizar contraseña\"}");
                    return;
                }
            }

            existente.setContrasena(null);
            ApiUtils.sendJson(ex, 200, gson.toJson(existente));

        } catch (IllegalArgumentException dup) { // from DAO
            ApiUtils.sendJson(ex, 409, "{\"error\":\"Usuario duplicado\"}");
        }
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
        else ApiUtils.sendJson(ex, 404, "{\"error\":\"No encontrado\"}");
    }

    private static String normUser(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? "" : t.toLowerCase(java.util.Locale.ROOT);
    }
}
