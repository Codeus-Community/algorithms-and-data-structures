package org.codeus.ws_deque.service;

import lombok.RequiredArgsConstructor;
import org.codeus.ws_deque.entity.Golfer;
import org.codeus.ws_deque.queue.WriteBehindQueue;
import org.codeus.ws_deque.repo.GolferRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author yelyzavetalubenets
 **/
@Service
@RequiredArgsConstructor
public class GolfersBatchUpdateService {
    private final GolferRepository golferRepo;
    private final WriteBehindQueue writeBehindQueue;
    private static final int THRESHOLD =5;

    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void flushWriteBehindQueue() {

        int queueSize = writeBehindQueue.size();

        if (queueSize >= THRESHOLD) {
            List<Golfer> golfers = writeBehindQueue.drain();
            if (!golfers.isEmpty()) {
                golferRepo.saveAll(golfers);
                System.out.println("✅ Flushed " + golfers.size() + " golfer score updates to DB (triggered by threshold)");
            }
        } else {
            System.out.println("⏳ Queue size (" + queueSize + ") below threshold (" + THRESHOLD + ")");
        }
    }
}
