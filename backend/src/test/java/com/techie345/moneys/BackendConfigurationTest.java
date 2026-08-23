package com.techie345.moneys;

import java.time.Duration;

import com.techie345.moneys.configuration.AppPropertiesConfiguration;
import com.techie345.moneys.configuration.CorsProperties;
import com.techie345.moneys.configuration.DatabaseProperties;
import com.techie345.moneys.configuration.LimitsProperties;
import com.techie345.moneys.configuration.SessionProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration",
                "app.database.url=jdbc:postgresql://db.example/moneys",
                "app.database.username=test-user",
                "app.database.password=test-password",
                "app.database.pool-size=12",
                "app.database.enabled=false",
                "app.financial.api.enabled=false",
                "app.session.timeout=45m",
                "app.session.cookie-name=test-session",
                "app.session.secure-cookie=true",
                "app.cors.allowed-origins=https://app.example",
                "app.cors.allowed-methods=GET,POST",
                "app.cors.allowed-headers=Content-Type,X-CSRF-TOKEN",
                "app.cors.allow-credentials=true",
                "app.limits.max-upload-bytes=1048576",
                "app.limits.max-page-size=50"
        })
@ActiveProfiles("test")
@Import(AppPropertiesConfiguration.class)
class BackendConfigurationTest {
    @Autowired
    private DatabaseProperties database;

    @Autowired
    private SessionProperties session;

    @Autowired
    private CorsProperties cors;

    @Autowired
    private LimitsProperties limits;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Autowired
    private CookieSerializer cookieSerializer;

    @Autowired
    private Environment environment;

    @Test
    void bindsTypedApplicationProperties() {
        assertThat(database.url()).isEqualTo("jdbc:postgresql://db.example/moneys");
        assertThat(database.poolSize()).isEqualTo(12);
        assertThat(session.timeout()).isEqualTo(Duration.ofMinutes(45));
        assertThat(session.cookieName()).isEqualTo("test-session");
        assertThat(session.secureCookie()).isTrue();
        assertThat(cookieSerializer).isNotNull();
        assertThat(cors.allowedOrigins()).containsExactly("https://app.example");
        assertThat(limits.maxUploadBytes()).isEqualTo(1048576L);
        assertThat(limits.maxPageSize()).isEqualTo(50);
        assertThat(environment.getProperty("spring.session.jdbc.initialize-schema")).isEqualTo("never");
        assertThat(environment.getProperty("management.server.address")).isEqualTo("127.0.0.1");
    }

    @Test
    void appliesConfiguredCorsToRequests() {
        HttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/accounts");

        CorsConfiguration configuration = corsConfigurationSource.getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins()).containsExactly("https://app.example");
        assertThat(configuration.getAllowedMethods()).containsExactly("GET", "POST");
        assertThat(configuration.getAllowCredentials()).isTrue();
        assertThat(configuration.checkOrigin("https://app.example")).isEqualTo("https://app.example");
        assertThat(configuration.checkOrigin("https://evil.example")).isNull();
        assertThat(corsConfigurationSource.getCorsConfiguration(
                new MockHttpServletRequest("GET", "/internal/status"))).isNull();
    }
}
