package org.example.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.time.Instant;

public class Jwt {
    private static final String SECRET = System.getenv("JWT_SECRET");
    private static final String ISSUER = System.getenv().getOrDefault("JWT_ISSUER","vpp-api");
    private static final long EXP_MIN = Long.parseLong(System.getenv().getOrDefault("JWT_EXP_MIN","120"));

    private static Algorithm alg() {
        if (SECRET == null || SECRET.length() < 16)
            throw new IllegalStateException("Falta JWT_SECRET (>=16 chars)");
        return Algorithm.HMAC256(SECRET);
    }

    public static String sign(int userId, String usuario, String rol) {
        Instant now = Instant.now();
        return JWT.create()
                .withIssuer(ISSUER)
                .withIssuedAt(now)
                .withExpiresAt(now.plusSeconds(EXP_MIN * 60))
                .withClaim("uid", userId)
                .withClaim("user", usuario)
                .withClaim("rol", rol)
                .sign(alg());
    }

    public static DecodedJWT verify(String token) {
        return JWT.require(alg()).withIssuer(ISSUER).build().verify(token);
    }
}
