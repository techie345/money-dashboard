package com.techie345.moneys.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;

@Configuration
@EnableJdbcHttpSession
@ConditionalOnProperty(name = "app.database.enabled", havingValue = "true", matchIfMissing = true)
public class JdbcSessionConfiguration {
}
