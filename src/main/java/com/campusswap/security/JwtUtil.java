package com.campusswap.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

/**
 * Issues and verifies HS256 JWTs.
 *
 * <p>Uses the JJWT 0.12.x API. The older {@code Jwts.parserBuilder()} entry point
 * was <em>removed</em> in 0.12.0 (not merely deprecated), so the current parser
 * chain is {@code Jwts.parser().verifyWith(key).build().parseSignedClaims(...)}.
 */
@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ---------------------------------------------------------------- issuing

    public String generateToken(UserDetails userDetails) {
        return createToken(userDetails.getUsername(), null);
    }

    public String generateToken(String username, String role) {
        return createToken(username, role);
    }

    /**
     * Builds the token using only {@code claim(String, Object)}, whose signature is
     * stable across JJWT versions, rather than the {@code claims(Map)} overload.
     *
     * <p>The role claim is informational for clients only. Authorization is always
     * re-derived from the database by CustomUserDetailsService on each request, so
     * a stale role in an already-issued token cannot escalate privileges.
     */
    private String createToken(String subject, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        var builder = Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate);

        if (role != null) {
            builder.claim("role", role);
        }

        return builder.signWith(getSigningKey(), Jwts.SIG.HS256).compact();
    }

    // --------------------------------------------------------------- reading

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // ------------------------------------------------------------ validation

    /**
     * Verifies signature and expiry. Returns false rather than throwing so the
     * authentication filter can fall through to the anonymous path, which the
     * entry point then converts into a 401.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("Rejected expired token");
            return false;
        } catch (SignatureException e) {
            log.warn("Rejected token with an invalid signature");
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Rejected malformed token: {}", e.getMessage());
            return false;
        }
    }

    /** Additionally confirms the token subject matches the resolved user. */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            return extractUsername(token).equals(userDetails.getUsername())
                    && validateToken(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token/user mismatch check failed: {}", e.getMessage());
            return false;
        }
    }
}
