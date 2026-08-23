package com.techie345.moneys.identity;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.mockito.Mockito;

import java.util.List;

@WebMvcTest(ProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "app.database.enabled=false")
@org.springframework.context.annotation.Import({ProfileControllerTest.Configuration.class, CurrentUserArgumentResolver.class})
class ProfileControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;

    @Test
    void returnsSafeCurrentUserFromMeAndProfile() throws Exception {
        UUID id = UUID.randomUUID();
        UserEntity user = Mockito.mock(UserEntity.class);
        when(user.toAccount()).thenReturn(new UserAccount(id, "provider-subject", "Alex", "alex@example.com",
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-02T00:00:00Z")));
        when(users.findById(id)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/v1/me").principal(() -> id.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.displayName").value("Alex"))
                .andExpect(jsonPath("$.googleSubject").doesNotExist());
        mockMvc.perform(get("/api/v1/profile").principal(() -> id.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alex@example.com"));
    }

    @Test
    void validatesProfilePatch() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(patch("/api/v1/profile").principal(() -> id.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @TestConfiguration
    static class Configuration implements WebMvcConfigurer {
        @Bean UserRepository userRepository() { return Mockito.mock(UserRepository.class); }
        @Override public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new CurrentUserArgumentResolver());
        }
    }
}
