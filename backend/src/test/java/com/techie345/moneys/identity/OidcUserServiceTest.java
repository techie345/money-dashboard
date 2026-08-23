package com.techie345.moneys.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OidcUserServiceTest {
    @Test
    void upsertsVerifiedSubjectAndUsesLocalIdAsPrincipalName() {
        UserRepository repository = mock(UserRepository.class);
        UUID id = UUID.randomUUID();
        UserEntity existing = spy(UserEntity.create("google-subject", "Old Name", "old@example.com"));
        when(existing.getId()).thenReturn(id);
        when(repository.findByGoogleSubject("google-subject")).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        var service = new OidcUserService(repository, request -> { throw new AssertionError("delegate not used"); });
        var oidc = mock(org.springframework.security.oauth2.core.oidc.user.OidcUser.class);
        when(oidc.getSubject()).thenReturn("google-subject");
        when(oidc.getFullName()).thenReturn("New Name");
        when(oidc.getEmail()).thenReturn("new@example.com");
        when(oidc.getIdToken()).thenReturn(new org.springframework.security.oauth2.core.oidc.OidcIdToken(
                "token", Instant.now(), Instant.now().plusSeconds(300), java.util.Map.of("sub", "google-subject")));

        var result = service.upsert(oidc);

        assertThat(result.getName()).isEqualTo(id.toString());
        verify(repository).save(existing);
    }

    @Test
    void createsLocalUserForUnseenVerifiedSubject() {
        UserRepository repository = mock(UserRepository.class);
        UUID id = UUID.randomUUID();
        when(repository.findByGoogleSubject("brand-new-subject")).thenReturn(Optional.empty());
        when(repository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity saved = invocation.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(saved, "id", id);
            return saved;
        });
        var service = new OidcUserService(repository, request -> { throw new AssertionError("delegate not used"); });
        var oidc = mock(org.springframework.security.oauth2.core.oidc.user.OidcUser.class);
        when(oidc.getSubject()).thenReturn("brand-new-subject");
        when(oidc.getFullName()).thenReturn("Fresh User");
        when(oidc.getEmail()).thenReturn("fresh@example.com");
        when(oidc.getIdToken()).thenReturn(new org.springframework.security.oauth2.core.oidc.OidcIdToken(
                "token", Instant.now(), Instant.now().plusSeconds(300), java.util.Map.of("sub", "brand-new-subject")));

        var result = service.upsert(oidc);

        assertThat(result.getName()).isEqualTo(id.toString());
        verify(repository).save(argThat(user ->
                "brand-new-subject".equals(user.getGoogleSubject())
                        && "Fresh User".equals(user.getDisplayName())
                        && "fresh@example.com".equals(user.getEmail())));
    }

    @Test
    void localUserExposesNoProviderTokenFields() {
        var tokenFields = java.util.Arrays.stream(UserEntity.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName)
                .filter(name -> name.toLowerCase().contains("token") || name.toLowerCase().contains("secret"))
                .toList();

        assertThat(tokenFields).isEmpty();
    }
}
