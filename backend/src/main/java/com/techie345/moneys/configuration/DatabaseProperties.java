package com.techie345.moneys.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@ConfigurationProperties("app.database")
@Validated
public record DatabaseProperties(
        boolean enabled,
        @NotBlank String url,
        @NotBlank String username,
        String password,
        @Min(1) int poolSize
) {
}
