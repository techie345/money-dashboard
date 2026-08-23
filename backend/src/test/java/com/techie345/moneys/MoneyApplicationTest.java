package com.techie345.moneys;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration",
        "app.database.enabled=false",
        "app.financial.api.enabled=false"
})
@ActiveProfiles("test")
class MoneyApplicationTest {
    @Test
    void starts() {
    }
}
