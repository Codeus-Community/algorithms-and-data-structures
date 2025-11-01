package org.codeus.jumphash.dashboard;

import org.codeus.jumphash.common.StatsSnapshot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsRepository repository;

    public StatsController(StatsRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public StatsSnapshot stats() {
        return repository.snapshot();
    }
}
