package com.techie345.moneys.financial.api;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;

class IfMatchParsingTest {
    @Test void acceptsStandardQuotedEtags() { assertThat(FinancialResourceController.parseVersion("\"7\"")).isEqualTo(7); }
    @Test void rejectsWeakEtags() { assertThatThrownBy(() -> FinancialResourceController.parseVersion("W/\"7\""))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("weak"); }
}
