package org.codeus.outbox.orderservice;

import com.codeus.outbox.orderservice.OrderServiceApplication;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest(
        classes = OrderServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class OutboxInboxE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldHaveEqualOrderAndInboxCount() {
        int ordersToCreate = 100;

        // Create several orders via REST
        for (int i = 0; i < ordersToCreate; i++) {
            ResponseEntity<Object> response = restTemplate.postForEntity("http://localhost:8080/api/orders", Map.of("description", "Test order " + i), Object.class);
            System.out.printf(
                    "➡️ Created order #%d | Status: %s | Body: %s%n",
                    i,
                    response.getStatusCode(),
                    response.getBody()
            );
        }

        // Wait a bit for async flow to complete
        Awaitility.await()
                .atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    Long ordersCount = restTemplate.getForObject("http://localhost:8080/api/orders/count", Long.class);
                    Long inboxCount = restTemplate.getForObject("http://localhost:8081/api/events/count", Long.class);

                    System.out.printf("📦 Orders count: %d | 📥 Inbox count: %d%n", ordersCount, inboxCount);

                    assertEquals(ordersCount, inboxCount,
                            "Inbox count should match created orders");
                });
    }
}
