package org.example.endpoints;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.example.dao.ProductoDAO;
import org.example.dao.ProcesoDAO;
import org.example.model.Producto;
import org.example.model.Proceso;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ProcesosHandler implements HttpHandler {
    private final Gson gson = ApiUtils.GSON;
    private final ProcesoDAO procesoDAO = new ProcesoDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            String method = ex.getRequestMethod();
            String path   = ex.getRequestURI().getPath(); // /procesos, /procesos/{nro}, /procesos/{nro}/cancel ...
            String[] parts = path.split("/");
            String nroPath = (parts.length >= 3) ? parts[2] : null;
            String action  = (parts.length >= 4) ? parts[3] : null;

            DecodedJWT jwt = Auth.requireAuth(ex);

            if ("GET".equalsIgnoreCase(method)) {
                // ENCARGADO o CAJERO: listar o ver uno
                Auth.requireRole(jwt, "ENCARGADO","CAJERO");
                if (nroPath == null) handleList(ex);
                else handleGetOne(ex, nroPath);
                return;
            }

            if ("POST".equalsIgnoreCase(method)) {
                // Solo ENCARGADO crea procesos
                Auth.requireRole(jwt, "ENCARGADO");
                handleCreate(ex);
                return;
            }

            if ("PUT".equalsIgnoreCase(method)) {
                if (nroPath == null) { ApiUtils.sendJson(ex, 400, "{\"error\":\"Falta nro_proceso\"}"); return; }

                if ("cancel".equalsIgnoreCase(action)) {
                    Auth.requireRole(jwt, "ENCARGADO","CAJERO");
                    handleChangeState(ex, nroPath, "CANCELADO");
                    return;
                }
                if ("confirm".equalsIgnoreCase(action)) {
                    Auth.requireRole(jwt, "CAJERO");
                    handleChangeState(ex, nroPath, "FINALIZADO");
                    return;
                }
                ApiUtils.sendJson(ex, 404, "{\"error\":\"Acción desconocida\"}");
                return;
            }

            ApiUtils.sendJson(ex, 405, "{\"error\":\"Método no permitido\"}");
        } catch (IllegalAccessException e) {
            ApiUtils.sendJson(ex, 401, "{\"error\":\"No autorizado\"}");
        } catch (Exception e) {
            e.printStackTrace();
            ApiUtils.sendJson(ex, 500, "{\"error\":\"Error interno\"}");
        }
    }

    // GET /procesos?estado=PROCESANDO|FINALIZADO|CANCELADO (opcional)
    private void handleList(HttpExchange ex) throws Exception {
        String estado = ApiUtils.queryParam(ex.getRequestURI().getQuery(), "estado");
        List<Proceso> lista = procesoDAO.findAll(estado);
        ApiUtils.sendJson(ex, 200, gson.toJson(lista));
    }

    // GET /procesos/{nro}
    private void handleGetOne(HttpExchange ex, String nro) throws Exception {
        Proceso p = procesoDAO.findByNro(nro);
        if (p == null) { ApiUtils.sendJson(ex, 404, "{\"error\":\"No encontrado\"}"); return; }
        ApiUtils.sendJson(ex, 200, gson.toJson(p));
    }

    // POST /procesos
    // Body esperado: { "nroProceso":"(opcional)", "fecha":"2025-09-01T18:30:00"(opcional),
    //                  "productoId":123, "pesoKg":15.2 }
    private void handleCreate(HttpExchange ex) throws Exception {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Proceso dto = gson.fromJson(body, Proceso.class);

        if (dto.getProductoId() == null || dto.getPesoKg() == null) {
            ApiUtils.sendJson(ex, 400, "{\"error\":\"productoId y pesoKg son requeridos\"}");
            return;
        }

        Producto prod = productoDAO.findById(dto.getProductoId());
        if (prod == null) { ApiUtils.sendJson(ex, 404, "{\"error\":\"Producto inexistente\"}"); return; }

        BigDecimal precioUnitario = prod.getPrecioKg();
        BigDecimal importe = precioUnitario.multiply(dto.getPesoKg());

        String nro = dto.getNroProceso();
        if (nro == null || nro.isBlank()) nro = generarNro();

        LocalDateTime fecha = (dto.getFecha() != null) ? dto.getFecha() : LocalDateTime.now();

        Proceso nuevo = new Proceso(
                null, nro, "PROCESANDO", fecha,
                dto.getProductoId(), dto.getPesoKg(),
                precioUnitario, importe
        );
        Proceso creado = procesoDAO.create(nuevo);

        ApiUtils.sendJson(ex, 201, gson.toJson(creado));
    }

    // PUT /procesos/{nro}/cancel  o  /procesos/{nro}/confirm
    private void handleChangeState(HttpExchange ex, String nro, String nuevoEstado) throws Exception {
        Proceso p = procesoDAO.findByNro(nro);
        if (p == null) { ApiUtils.sendJson(ex, 404, "{\"error\":\"No encontrado\"}"); return; }

        boolean ok = procesoDAO.actualizarEstado(nro, nuevoEstado);
        if (ok) {
            p.setEstado(nuevoEstado);
            ApiUtils.sendJson(ex, 200, gson.toJson(p));
        } else {
            ApiUtils.sendJson(ex, 500, "{\"error\":\"No se pudo actualizar\"}");
        }
    }


    private String generarNro() {
        Random r = new Random();
        int n = 100_000_000 + r.nextInt(900_000_000);
        return String.valueOf(n);
    }
}
