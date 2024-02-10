package com.riequation.property.webserver;

import io.jsonwebtoken.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.Date;

@CrossOrigin(origins = "*")
public class JwtUtil {

    private static final String SECRET_KEY = "riequation";

    public static String generateToken(String userId) {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        long expMillis = nowMillis + 3600000; // Token validity (1 hour, for example)
        Date exp = new Date(expMillis);

        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(SignatureAlgorithm.HS512, SECRET_KEY)
                .compact();
    }

    public static Jws<Claims> validateToken(String token) {
        return Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token);
    }
}
