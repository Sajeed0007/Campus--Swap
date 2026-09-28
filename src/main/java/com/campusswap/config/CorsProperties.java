package com.campusswap.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Externalised CORS settings so deployed environments can declare their own
 * frontend origins without a code change.
 */
@Component
@ConfigurationProperties(prefix = "app.cors")
@Getter
@Setter
public class CorsProperties {

    /**
     * Browser origins permitted to call the API. Wildcards are supported via
     * allowedOriginPatterns (for example https://*.campusswap.com).
     */
    private List<String> allowedOrigins = List.of("http://localhost:3000");
}
