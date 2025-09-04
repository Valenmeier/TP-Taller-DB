package org.example.endpoints;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.dao.ProductoDAO;
import org.example.model.Producto;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

public class ProductHandler implements HttpHandler {
    private final Gson gson = ApiUtils.GSON;
    private final ProductoDAO dao = new ProductoDAO();

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            final String method = ex.getRequestMethod();
            final String path = ex.getRequestURI().getPath();
            final Integer id = extractId(path);

            DecodedJWT jwt = Auth.requireAuth(ex);

            if ("GET".equalsIgnoreCase(method)) {
                Auth.requireRole(jwt, "ENCARGADO","ADMIN");
                if (id == null) handleListBySeccion(ex); else handleGetOne(ex, id);
                return;
            }

            Auth.requireRole(jwt, "ADMIN");

            switch (method) {
                case "POST" -> handleCreate(ex);
                case "PUT"  -> { if (id==null){ApiUtils.sendJson(ex,400,"{\"error\":\"Falta id\"}");}
                else handleUpdate(ex, id); }
                case "DELETE" -> { if (id==null){ApiUtils.sendJson(ex,400,"{\"error\":\"Falta id\"}");}
                else handleDelete(ex, id); }
                default -> ApiUtils.sendJson(ex, 405, "{\"error\":\"Método no permitido\"}");
            }
        } catch (IllegalAccessException e) {
            ApiUtils.sendJson(ex, 401, "{\"error\":\"No autorizado\"}");
        } catch (Exception e) {
            e.printStackTrace();
            ApiUtils.sendJson(ex, 500, "{\"error\":\"Error interno\"}");
        }
    }


    private void handleListBySeccion(HttpExchange ex) throws Exception {
        String seccion = ApiUtils.queryParam(ex.getRequestURI().getQuery(), "seccion");
        if (seccion == null || seccion.isBlank()) {
            ApiUtils.sendJson(ex, 400, "{\"error\":\"Parametro 'seccion' requerido\"}");
            return;
        }
        List<Producto> lista = dao.findAll(seccion);   // ya tenés este método
        ApiUtils.sendJson(ex, 200, gson.toJson(lista));
    }

    // GET /productos/{id}
    private void handleGetOne(HttpExchange ex, int id) throws Exception {
        Producto p = dao.findById(id);
        if (p == null) { ApiUtils.sendJson(ex, 404, "{\"error\":\"No encontrado\"}"); return; }
        ApiUtils.sendJson(ex, 200, gson.toJson(p));
    }

    // POST /productos  Body: { "nombre":"Aguja","seccion":"CARNES","precioKg":1500.00 }
    private void handleCreate(HttpExchange ex) throws Exception {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Producto dto = gson.fromJson(body, Producto.class);
        if (!validNuevo(dto)) {
            ApiUtils.sendJson(ex, 400, "{\"error\":\"nombre, seccion y precioKg son requeridos\"}");
            return;
        }
        try {
            var creado = dao.create(new Producto(null, dto.getNombre(), dto.getSeccion(), dto.getPrecioKg()));
            ApiUtils.sendJson(ex, 201, gson.toJson(creado));
        } catch (SQLIntegrityConstraintViolationException dup) {
            ApiUtils.sendJson(ex, 409, "{\"error\":\"Producto duplicado (nombre+seccion)\"}");
        }
    }

    // PUT /productos/{id}  Body parcial o completo
    private void handleUpdate(HttpExchange ex, int id) throws Exception {
        Producto existente = dao.findById(id);
        if (existente == null) { ApiUtils.sendJson(ex, 404, "{\"error\":\"No encontrado\"}"); return; }

        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Producto dto = gson.fromJson(body, Producto.class);

        if (dto.getNombre()!=null && !dto.getNombre().isBlank()) existente.setNombre(dto.getNombre());
        if (dto.getSeccion()!=null && !dto.getSeccion().isBlank()) existente.setSeccion(dto.getSeccion());
        if (dto.getPrecioKg()!=null && dto.getPrecioKg().compareTo(BigDecimal.ZERO)>0) existente.setPrecioKg(dto.getPrecioKg());

        try {
            boolean ok = dao.update(existente);
            if (ok) ApiUtils.sendJson(ex, 200, gson.toJson(existente));
            else    ApiUtils.sendJson(ex, 500, "{\"error\":\"No se pudo actualizar\"}");
        } catch (SQLIntegrityConstraintViolationException dup) {
            ApiUtils.sendJson(ex, 409, "{\"error\":\"Producto duplicado (nombre+seccion)\"}");
        }
    }

    // DELETE /productos/{id}
    private void handleDelete(HttpExchange ex, int id) throws Exception {
        boolean ok = dao.delete(id);
        if (ok) ApiUtils.sendJson(ex, 200, "{\"ok\":true}");
        else    ApiUtils.sendJson(ex, 404, "{\"error\":\"No encontrado\"}");
    }

    // Helpers
    private Integer extractId(String path) {
        String[] parts = path.split("/");
        if (parts.length >= 3 && !parts[2].isBlank()) {
            try { return Integer.parseInt(parts[2]); } catch (NumberFormatException ignored) {}
        }
        return null;
    }
    private boolean validNuevo(Producto p) {
        return p!=null
                && p.getNombre()!=null && !p.getNombre().isBlank()
                && p.getSeccion()!=null && !p.getSeccion().isBlank()
                && p.getPrecioKg()!=null && p.getPrecioKg().compareTo(BigDecimal.ZERO)>0;
    }
}
