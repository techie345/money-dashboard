package com.techie345.moneys.identity;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.*;

@WebMvcTest(controllers = SecurityConfigTest.ProbeController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.database.enabled=false",
        "app.cors.allowed-origins=https://app.example",
        "app.cors.allowed-methods=GET,POST",
        "app.cors.allowed-headers=Content-Type,X-CSRF-TOKEN",
        "app.cors.allow-credentials=true",
        "spring.security.oauth2.client.registration.google.client-id=test-client",
        "spring.security.oauth2.client.registration.google.client-secret=test-secret",
        "spring.security.oauth2.client.registration.google.scope=openid,profile,email",
        "spring.security.oauth2.client.provider.google.authorization-uri=https://accounts.example/auth",
        "spring.security.oauth2.client.provider.google.token-uri=https://accounts.example/token",
        "spring.security.oauth2.client.provider.google.jwk-set-uri=https://accounts.example/jwks",
        "spring.security.oauth2.client.provider.google.issuer-uri=https://accounts.example"
})
class SecurityConfigTest {
    @Autowired MockMvc mockMvc;

    @Test
    void rejectsAnonymousApiRequests() throws Exception {
        mockMvc.perform(get("/api/v1/security-probe")).andExpect(status().isUnauthorized());
    }

    @Test
    void permitsHealthAndOauthEntrypoints() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isNotFound());
        mockMvc.perform(get("/oauth2/authorization/google")).andExpect(status().is3xxRedirection());
    }

    @Test
    void protectsCookieMutationWithCsrfAndUsesAuthenticatedPrincipal() throws Exception {
        String userId = UUID.randomUUID().toString();
        mockMvc.perform(post("/api/v1/security-probe").with(user(userId)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/security-probe").with(user(userId)).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void allowsConfiguredCorsAndRejectsUnknownOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/security-probe").header("Origin", "https://app.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());
        mockMvc.perform(options("/api/v1/security-probe").header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @RestController
    @RequestMapping("/api/v1/security-probe")
    static class ProbeController {
        @GetMapping String get() { return "ok"; }
        @PostMapping String post() { return "ok"; }
    }
}
