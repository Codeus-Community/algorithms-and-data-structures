package com.codeus.inbox.postorderservice.service;

import com.codeus.inbox.postorderservice.entity.InboxEvent;
import com.codeus.inbox.postorderservice.entity.InboxEventStatus;
import com.codeus.inbox.postorderservice.repository.InboxEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostOrderService {

    private final InboxEventRepository inboxEventRepository;

    public long count() {
        return inboxEventRepository.count();
    }

    public void process(InboxEvent event) {
        int random = ThreadLocalRandom.current().nextInt(1, 11);
        if (random <= 4) { // 40% chance of failure
            throw new RuntimeException("Simulated random failure for testing");
        }
        // some business logic...
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processInbox() {
        // todo: implement retry for failed events from inbox
    }
}
