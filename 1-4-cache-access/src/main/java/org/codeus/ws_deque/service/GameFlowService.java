package org.codeus.ws_deque.service;

import lombok.RequiredArgsConstructor;
import org.codeus.ws_deque.dto.UpdateScoreRequest;
import org.codeus.ws_deque.entity.GolfTournament;
import org.codeus.ws_deque.entity.Golfer;
import org.codeus.ws_deque.repo.GolfTournamentRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author yelyzavetalubenets
 **/
@Service
@RequiredArgsConstructor
public class GameFlowService {

    private final GolfTournamentRepository tournamentRepo;
    private final GolfTournamentService tournamentService;
    private final Random random = new Random();

    @Scheduled(fixedRate = 60000)
    public void simulateLiveScoreUpdates() {
        List<GolfTournament> tournaments = tournamentRepo.findAll();
        if (tournaments.isEmpty()) return;

        boolean anyUpdated = false;

        for(GolfTournament tournament: tournaments) {

            for (Golfer golfer : tournament.getGolfers()) {
                Map<Integer, Integer> scores = golfer.getHoleScores();
                int playedHoles = scores.size();

                if (playedHoles >= 18) {
                    System.out.printf("✅ %s finished all 18 holes, skipping.%n", golfer.getName());
                    continue;
                }

                int hole;
                do {
                    hole = random.nextInt(18) + 1;
                } while (scores.containsKey(hole));

                int score = random.nextInt(6);

                tournamentService.updateGolferScore(
                        tournament.getId(),
                        new UpdateScoreRequest(golfer.getId(), hole, score)
                );

                System.out.printf("⛳ %s played hole %d → %d strokes%n", golfer.getName(), hole, score);
                anyUpdated = true;
            }
        }

        if(!anyUpdated){
            System.out.println("🏁 All golfers finished. Scheduler will skip updates.");
        }
    }
}
