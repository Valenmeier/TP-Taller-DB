package org.example.endpoints;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.sun.net.httpserver.HttpExchange;
import org.example.security.Jwt;

public class Auth {
    public static DecodedJWT requireAuth(HttpExchange ex) throws IllegalAccessException {
        String h = ex.getRequestHeaders().getFirst("Authorization");
        if (h == null || !h.startsWith("Bearer ")) throw new IllegalAccessException("Sin token");
        String token = h.substring("Bearer ".length()).trim();
        return Jwt.verify(token);
    }

    public static void requireRole(DecodedJWT jwt, String... roles) throws IllegalAccessException {
        String rol = jwt.getClaim("rol").asString();
        for (String r : roles) if (r.equals(rol)) return;
        throw new IllegalAccessException("Rol no autorizado");
    }
}
