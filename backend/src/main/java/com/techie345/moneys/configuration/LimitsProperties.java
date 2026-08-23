package com.techie345.moneys.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;

@ConfigurationProperties("app.limits")
@Validated
public record LimitsProperties(
        @Min(1) long maxUploadBytes,
        @Min(1) int maxPageSize
) {
}
