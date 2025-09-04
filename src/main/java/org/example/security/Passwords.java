package org.example.security;

import org.mindrot.jbcrypt.BCrypt;

public class Passwords {
    private static final int WORK_FACTOR = 12;

    public static String hash(String plain) {
        if (plain == null || plain.isBlank()) {
            throw new IllegalArgumentException("La contraseña no puede ser vacía");
        }
        return BCrypt.hashpw(plain, BCrypt.gensalt(WORK_FACTOR));
    }

    public static boolean verify(String plain, String hash) {
        return hash != null && !hash.isBlank() && BCrypt.checkpw(plain, hash);
    }
}
