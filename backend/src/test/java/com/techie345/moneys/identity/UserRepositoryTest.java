package com.techie345.moneys.identity;

import java.util.UUID;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class UserRepositoryTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("app.database.url", POSTGRES::getJdbcUrl);
        registry.add("app.database.username", POSTGRES::getUsername);
        registry.add("app.database.password", POSTGRES::getPassword);
        registry.add("app.database.enabled", () -> "true");
        registry.add("spring.session.jdbc.initialize-schema", () -> "always");
    }

    @Autowired
    private UserRepository repository;

    @Test
    void savesAndLoadsUserByGoogleSubject() {
        UserEntity saved = repository.save(UserEntity.create(
                "google-subject-1", "Alex Example", "alex@example.com"));

        Optional<UserEntity> loaded = repository.findByGoogleSubject("google-subject-1");

        assertThat(saved.getId()).isNotNull();
        assertThat(loaded).isPresent();
        assertThat(loaded.orElseThrow().getDisplayName()).isEqualTo("Alex Example");
        assertThat(loaded.orElseThrow().getEmail()).isEqualTo("alex@example.com");
        assertThat(loaded.orElseThrow().getCreatedAt()).isNotNull();
        assertThat(loaded.orElseThrow().getUpdatedAt()).isNotNull();
    }

    @Test
    void readsOnlyTheRequestedUserId() {
        UserEntity first = repository.save(UserEntity.create(
                "google-subject-2", "First User", "first@example.com"));
        UserEntity second = repository.save(UserEntity.create(
                "google-subject-3", "Second User", "second@example.com"));

        assertThat(repository.findById(first.getId())).map(UserEntity::getId).contains(first.getId());
        assertThat(repository.findById(second.getId())).map(UserEntity::getId).contains(second.getId());
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void rejectsDuplicateGoogleSubjects() {
        repository.saveAndFlush(UserEntity.create(
                "google-subject-unique", "First User", "first@example.com"));

        assertThatThrownBy(() -> repository.saveAndFlush(UserEntity.create(
                "google-subject-unique", "Second User", "second@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
