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
import java.util.Locale;

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
                if (id == null) handleList(ex); else handleGetOne(ex, id);
                return;
            }

            Auth.requireRole(jwt, "ADMIN");

            switch (method) {
                case "POST" -> handleCreate(ex);
                case "PUT"  -> {
                    if (id == null) {
                        ApiUtils.sendJson(ex, 400, "{\"error\":\"Falta id\"}");
                    } else {
                        handleUpdate(ex, id);
                    }
                }
                case "DELETE" -> {
                    if (id == null) {
                        ApiUtils.sendJson(ex, 400, "{\"error\":\"Falta id\"}");
                    } else {
                        handleDelete(ex, id);
                    }
                }
                default -> ApiUtils.sendJson(ex, 405, "{\"error\":\"Método no permitido\"}");
            }
        } catch (IllegalAccessException e) {
            ApiUtils.sendJson(ex, 401, "{\"error\":\"No autorizado\"}");
        } catch (Exception e) {
            e.printStackTrace();
            ApiUtils.sendJson(ex, 500, "{\"error\":\"Error interno\"}");
        }
    }

    // GET
    private void handleList(HttpExchange ex) throws Exception {
        String seccionRaw = ApiUtils.queryParam(ex.getRequestURI().getQuery(), "seccion");
        String seccion = normSeccion(seccionRaw);
        List<Producto> lista = dao.findAll(seccion);
        ApiUtils.sendJson(ex, 200, gson.toJson(lista));
    }

    // GET /productos/{id}
    private void handleGetOne(HttpExchange ex, int id) throws Exception {
        Producto p = dao.findById(id);
        if (p == null) {
            ApiUtils.sendJson(ex, 404, "{\"error\":\"No encontrado\"}");
            return;
        }
        ApiUtils.sendJson(ex, 200, gson.toJson(p));
    }

    // POST /productos
    private void handleCreate(HttpExchange ex) throws Exception {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Producto dto = gson.fromJson(body, Producto.class);

        if (dto != null) dto.setSeccion(normSeccion(dto.getSeccion()));

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

    // PUT /productos/{id}
    private void handleUpdate(HttpExchange ex, int id) throws Exception {
        Producto existente = dao.findById(id);
        if (existente == null) {
            ApiUtils.sendJson(ex, 404, "{\"error\":\"No encontrado\"}");
            return;
        }

        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Producto dto = gson.fromJson(body, Producto.class);

        if (dto.getNombre()!=null && !dto.getNombre().isBlank()) existente.setNombre(dto.getNombre());
        if (dto.getSeccion()!=null && !dto.getSeccion().isBlank()) existente.setSeccion(normSeccion(dto.getSeccion()));
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
        int enCurso = dao.contarProcesosEnCurso(id);
        if (enCurso > 0) {
            ApiUtils.sendJson(ex, 409, "{\"error\":\"No se puede desactivar: hay procesos en PROCESANDO\"}");
            return;
        }
        boolean ok = dao.delete(id);
        if (ok) ApiUtils.sendJson(ex, 200, "{\"ok\":true, \"accion\":\"desactivado\"}");
        else    ApiUtils.sendJson(ex, 404, "{\"error\":\"No encontrado o ya estaba desactivado\"}");
    }
    // Helpers
    private Integer extractId(String path) {
        String[] parts = path.split("/");
        if (parts.length >= 3 && !parts[2].isBlank()) {
            try { return Integer.parseInt(parts[2]); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private static String normSeccion(String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        return trimmed.isEmpty() ? "" : trimmed.toUpperCase(Locale.ROOT);
    }

    private boolean validNuevo(Producto p) {
        return p!=null
                && p.getNombre()!=null && !p.getNombre().isBlank()
                && p.getSeccion()!=null && !p.getSeccion().isBlank()
                && p.getPrecioKg()!=null && p.getPrecioKg().compareTo(BigDecimal.ZERO)>0;
    }
}
