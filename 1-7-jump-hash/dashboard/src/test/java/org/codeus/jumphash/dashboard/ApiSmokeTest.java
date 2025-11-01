package org.codeus.jumphash.dashboard;

import org.codeus.jumphash.common.ConsumerInstanceStats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
@AutoConfigureMockMvc
@EmbeddedKafka(topics = "demo.stats")
class ApiSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StatsRepository repository;

    @Test
    @DisplayName("GET /api/stats returns aggregated consumer metrics")
    void statsEndpointReturnsSnapshot() throws Exception {
        repository.updateConsumer("local-8091", new ConsumerInstanceStats(42, 0, System.currentTimeMillis(), 0));

        mockMvc.perform(get("/api/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consumers['local-8091'].processed").value(42))
                .andExpect(jsonPath("$.consumers['local-8091'].lag").value(0));
    }
}
