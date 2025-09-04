// CorsFilter.java
package org.example;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class CorsFilter extends Filter {
    @Override
    public void doFilter(HttpExchange ex, Chain chain) throws IOException {
        String allowed = System.getenv().getOrDefault("CORS_ORIGIN", "*");
        String origin = ex.getRequestHeaders().getFirst("Origin");

        Headers rh = ex.getResponseHeaders();
        if ("*".equals(allowed) || allowed.equals(origin)) {
            rh.set("Access-Control-Allow-Origin", origin != null ? origin : allowed);
        }
        rh.set("Vary", "Origin");
        rh.set("Access-Control-Allow-Headers", "Authorization, Content-Type");
        rh.set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        rh.set("Access-Control-Allow-Credentials", "true");
        rh.set("Access-Control-Max-Age", "86400");

        // --- Preflight ---
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            ex.close();
            return;
        }

        chain.doFilter(ex);
    }

    @Override
    public String description() {
        return "CORS preflight + headers";
    }
}
