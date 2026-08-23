package com.techie345.moneys.imports;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.UUID;
import java.util.List;
import com.techie345.moneys.imports.provider.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ImportController.class)
@TestPropertySource(properties = {"app.database.enabled=false", "app.imports.enabled=true", "app.financial.api.enabled=true"})
class ImportControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean ImportService service;

    @Test
    void createsPreviewGetsPreviewCommitsOnceAndDiscardsForOwner() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        when(service.create(any(), eq(owner))).thenReturn(new ImportResponse(id, "token", 0, "PREVIEW"));
        when(service.preview(id, owner)).thenReturn(new ImportResponse(id, "token", 0, "PREVIEW"));
        when(service.commit(id, owner, "token", 0)).thenReturn(new ImportResponse(id, "token", 1, "COMMITTED"));

        mockMvc.perform(multipart("/api/v1/imports").file("file", "Date,Description,Amount\n2026-01-01,Store,-2.00".getBytes())
                        .param("profile", "generic").principal(() -> owner.toString()))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/imports/{id}/preview", id).principal(() -> owner.toString()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/imports/{id}/commit", id).principal(() -> owner.toString())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"confirmationToken\":\"token\",\"version\":0}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/imports/{id}", id).principal(() -> owner.toString()))
                .andExpect(status().isNoContent());
    }

    @Test
    void otherUserCannotAccessImport() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.preview(eq(id), any())).thenThrow(new ImportNotFoundException());
        mockMvc.perform(get("/api/v1/imports/{id}/preview", id).principal(() -> UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsFullPreviewAndStructuredErrors() throws Exception {
        UUID owner = UUID.randomUUID(); UUID id = UUID.randomUUID();
        var preview = new ProviderPreview(new ProviderMetadata("x.csv", "generic", java.time.Instant.now(), "csv"), List.of(),
                List.of(new ImportIssue(2, "bad date", "x")), List.of(), java.util.Map.of(), List.of(), java.util.Map.of());
        when(service.preview(id, owner)).thenReturn(new ImportResponse(id, "token", 0, "PREVIEW", preview));
        mockMvc.perform(get("/api/v1/imports/{id}/preview", id).principal(() -> owner.toString()))
                .andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.preview.malformedRows[0].reason").value("bad date"));
    }

    @Test
    void mapsUnsupportedProviderToStableValidationError() throws Exception {
        UUID owner = UUID.randomUUID();
        when(service.create(any(), eq(owner))).thenThrow(new com.techie345.moneys.imports.provider.UnsupportedProviderException(ProviderType.CSV));

        mockMvc.perform(multipart("/api/v1/imports").file("file", "data".getBytes())
                        .principal(() -> owner.toString()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value("IMPORT_PROVIDER_UNSUPPORTED"));
    }
}
