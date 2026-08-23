package com.techie345.moneys.configuration;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@ConfigurationProperties("app.session")
@Validated
public record SessionProperties(
        @NotNull Duration timeout,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]+") String cookieName,
        boolean secureCookie
) {
}
