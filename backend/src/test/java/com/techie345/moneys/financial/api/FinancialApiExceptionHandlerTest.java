package com.techie345.moneys.financial.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FinancialApiExceptionHandlerTest {
    @Test
    void includesCurrentSafeRepresentationInConflictDetails() {
        var response = new FinancialApiExceptionHandler()
                .conflict(new VersionConflictException(new FinancialResourceDtos.AssetResponse(null, null,
                        "Home", null, 100, 3)));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().details()).hasSize(1);
        assertThat(response.getBody().details().getFirst()).isInstanceOf(java.util.Map.class);
        assertThat(((java.util.Map<?, ?>) response.getBody().details().getFirst()).get("current"))
                .isNotNull();
    }
}
