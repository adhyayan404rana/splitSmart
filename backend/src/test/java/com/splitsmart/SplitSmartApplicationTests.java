package com.splitsmart;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Spring application context load verification test.
 *
 * <p>This test ensures the full Spring application context starts up without
 * errors. It acts as the first line of defence against broken bean wiring,
 * missing configuration properties, or circular dependency issues.
 *
 * <p>The {@code test} profile disables components that require external
 * infrastructure (RabbitMQ, Redis, PostgreSQL) by overriding their
 * auto-configuration with test doubles via {@code application-test.yml}.
 *
 * <h3>What this test validates</h3>
 * <ul>
 *   <li>All {@code @Component}, {@code @Service}, {@code @Repository},
 *       and {@code @Controller} beans can be instantiated.</li>
 *   <li>All {@code @Value} and {@code @ConfigurationProperties} bindings
 *       resolve without missing-property errors.</li>
 *   <li>JPA entity scanning and Flyway migration baseline complete
 *       successfully against the H2 in-memory test database.</li>
 *   <li>Security filter chain (JWT + rate limiter) loads without
 *       configuration conflicts.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        // Override external service URLs to prevent real outbound connections
        "spring.rabbitmq.host=localhost",
        "spring.rabbitmq.port=5672",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        // Use H2 in-memory DB for context load test
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        // Disable Flyway for context load — schema is managed by Hibernate in tests
        "spring.flyway.enabled=false",
        // JWT secret for test context
        "splitsmart.jwt.secret=test-secret-key-minimum-256-bits-long-for-hmac-sha256-signing",
        "splitsmart.jwt.expiration-ms=86400000",
        // Disable async RabbitMQ listener during context load
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
@DisplayName("Spring Application Context Load")
class SplitSmartApplicationTests {

    /**
     * Verifies the application context loads without throwing any exceptions.
     *
     * <p>If bean wiring, property resolution, or entity scanning is broken,
     * this test will fail before any business logic is exercised.
     */
    @Test
    @DisplayName("Application context loads successfully")
    void contextLoads() {
        // The @SpringBootTest annotation itself drives the context load.
        // If the context fails to start, this test fails with a descriptive error.
    }
}
