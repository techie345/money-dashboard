package com.techie345.moneys;

import java.util.Arrays;

import com.techie345.moneys.shared.RequestCorrelationFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration",
        "app.database.enabled=false",
        "app.financial.api.enabled=false"
})
@ActiveProfiles("test")
class ActuatorTest {
    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.core.env.Environment environment;

    @Test
    void exposesOnlySafeActuatorEndpointsOnLoopback() {
        assertThat(environment.getProperty("management.server.address")).isEqualTo("127.0.0.1");
        assertThat(environment.getProperty("management.endpoints.web.exposure.include"))
                .isEqualTo("health,info,metrics");
        assertThat(environment.getProperty("management.endpoint.health.show-details")).isEqualTo("never");
        assertThat(Arrays.stream(environment.getProperty(
                "management.endpoints.web.exposure.include", "").split(",")))
                .containsExactlyInAnyOrder("health", "info", "metrics")
                .doesNotContain("env", "configprops", "beans");
    }

    @Test
    void echoesProvidedRequestCorrelationId() throws Exception {
        RequestCorrelationFilter filter = new RequestCorrelationFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestCorrelationFilter.HEADER_NAME, "request-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> { };

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(RequestCorrelationFilter.HEADER_NAME)).isEqualTo("request-123");
    }
}
