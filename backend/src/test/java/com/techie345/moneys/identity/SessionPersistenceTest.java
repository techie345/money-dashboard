package com.techie345.moneys.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.session.Session;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class SessionPersistenceTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("app.database.url", POSTGRES::getJdbcUrl);
        registry.add("app.database.username", POSTGRES::getUsername);
        registry.add("app.database.password", POSTGRES::getPassword);
        registry.add("app.database.enabled", () -> "true");
        registry.add("spring.session.jdbc.initialize-schema", () -> "never");
    }

    @Autowired
    private JdbcIndexedSessionRepository sessions;

    @Test
    void storesSessionInPostgres() {
        var jdbcSession = sessions.createSession();
        Session session = jdbcSession;
        session.setAttribute("localUserId", "user-123");
        sessions.save(jdbcSession);

        Session loaded = sessions.findById(session.getId());

        assertThat(loaded).isNotNull();
        assertThat((String) loaded.getAttribute("localUserId")).isEqualTo("user-123");
    }
}
