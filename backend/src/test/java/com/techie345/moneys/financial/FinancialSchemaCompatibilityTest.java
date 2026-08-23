package com.techie345.moneys.financial;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

class FinancialSchemaCompatibilityTest {
    @Test
    void migrationContainsMappedTransactionMetadataAndInvestmentColumns() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/db/migration/V1__initial_schema.sql")) {
            assertThat(stream).isNotNull();
            String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(sql).contains("review_status VARCHAR(16)", "transfer_type VARCHAR(40)", "imported_at TIMESTAMPTZ", "action VARCHAR(200)", "symbol VARCHAR(32)", "shares NUMERIC(24, 8)", "price_cents BIGINT");
        }
    }
}
