package com.campusswap.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Fails fast if a production deployment is still using the committed development
 * JWT secret, or a secret too short to be safe for HS256.
 */
@Component
@Profile("prod")
@RequiredArgsConstructor
@Slf4j
public class JwtSecretValidator {

    /** The value committed in application.yml. Must never reach production. */
    private static final String DEV_SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    /** HS256 requires a key of at least 256 bits. */
    private static final int MIN_SECRET_BYTES = 32;

    @Value("${jwt.secret}")
    private String secret;

    @PostConstruct
    void validate() {
        if (DEV_SECRET.equals(secret)) {
            throw new IllegalStateException(
                    "JWT_SECRET is still set to the development default. "
                            + "Set the JWT_SECRET environment variable to a unique random value.");
        }

        int length = secret == null ? 0 : secret.getBytes(StandardCharsets.UTF_8).length;
        if (length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least " + MIN_SECRET_BYTES
                            + " bytes for HS256, but was " + length + " bytes.");
        }

        log.info("JWT secret validated for production profile.");
    }
}
