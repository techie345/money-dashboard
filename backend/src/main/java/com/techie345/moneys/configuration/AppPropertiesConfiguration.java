package com.techie345.moneys.configuration;

import com.techie345.moneys.identity.CurrentUserArgumentResolver;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.List;

@Configuration
@EnableConfigurationProperties({
        DatabaseProperties.class,
        SessionProperties.class,
        CorsProperties.class,
        LimitsProperties.class
})
public class AppPropertiesConfiguration implements WebMvcConfigurer {
    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    public AppPropertiesConfiguration(CurrentUserArgumentResolver currentUserArgumentResolver) {
        this.currentUserArgumentResolver = currentUserArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<org.springframework.web.method.support.HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "app.database.enabled", havingValue = "true", matchIfMissing = true)
    HikariDataSource dataSource(DatabaseProperties properties) {
        HikariConfig configuration = new HikariConfig();
        configuration.setJdbcUrl(properties.url());
        configuration.setUsername(properties.username());
        configuration.setPassword(properties.password());
        configuration.setMaximumPoolSize(properties.poolSize());
        return new HikariDataSource(configuration);
    }

    @Bean
    CookieSerializer cookieSerializer(SessionProperties properties) {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName(properties.cookieName());
        serializer.setUseSecureCookie(properties.secureCookie());
        serializer.setSameSite("Lax");
        return serializer;
    }

    @Bean
    SessionRepositoryCustomizer<JdbcIndexedSessionRepository> sessionRepositoryCustomizer(
            SessionProperties properties) {
        return repository -> repository.setDefaultMaxInactiveInterval(properties.timeout());
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.allowedOrigins());
        configuration.setAllowedMethods(properties.allowedMethods());
        configuration.setAllowedHeaders(properties.allowedHeaders());
        configuration.setAllowCredentials(properties.allowCredentials());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
