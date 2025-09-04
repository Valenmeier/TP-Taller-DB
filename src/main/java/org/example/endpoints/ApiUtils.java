// ApiUtils.java
package org.example.endpoints;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public class ApiUtils {

    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, t, c) -> new JsonPrimitive(src.toString()))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, t, c) -> LocalDateTime.parse(json.getAsString()))
            .create();

    public static void sendJson(HttpExchange ex, int status, String json) throws IOException {
        //CORS
        String allowed = System.getenv().getOrDefault("CORS_ORIGIN", "*");
        String origin = ex.getRequestHeaders().getFirst("Origin");
        if (allowed.equals("*") || (allowed.equals(origin))) {
            ex.getResponseHeaders().add("Access-Control-Allow-Origin", origin != null ? origin : allowed);
        }
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Authorization, Content-Type");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");

        // --- Preflight OPTIONS ---
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            ex.close();
            return;
        }

        // --- JSON response ---
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static String queryParam(String query, String key) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key))
                return java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
        }
        return null;
    }
}
