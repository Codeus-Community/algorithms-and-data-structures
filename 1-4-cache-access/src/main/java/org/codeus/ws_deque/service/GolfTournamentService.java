package org.codeus.ws_deque.service;

import lombok.RequiredArgsConstructor;
import org.codeus.ws_deque.dto.GolfTournamentRequest;
import org.codeus.ws_deque.dto.GolferRequest;
import org.codeus.ws_deque.dto.UpdateScoreRequest;
import org.codeus.ws_deque.dto.cache.GolfTournamentInfo;
import org.codeus.ws_deque.dto.cache.GolferDto;
import org.codeus.ws_deque.dto.cache.GolferInfo;
import org.codeus.ws_deque.entity.GolfTournament;
import org.codeus.ws_deque.entity.Golfer;
import org.codeus.ws_deque.queue.WriteBehindQueue;
import org.codeus.ws_deque.repo.GolfTournamentRepository;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * @author yelyzavetalubenets
 **/
@Service
@RequiredArgsConstructor
public class GolfTournamentService {

    private final GolfTournamentRepository tournamentRepo;
    private final WriteBehindQueue writeBehindQueue;
    private final Random random = new Random();

    public GolfTournament addTournament(final GolfTournamentRequest request){
        final GolfTournament golfTournament = new GolfTournament();
        golfTournament.setName(request.name());
        golfTournament.setLocation(request.location());
        return tournamentRepo.save(golfTournament);
    }
    /** 🟢 READ-THROUGH
     TODO: Add Cacheable annotation: cache name - tournaments, key - #id
     **/
    public GolfTournamentInfo getTournament(final Long id) {
        System.out.println("📥 Read-through: loading tournament from DB");
        return generateTournamentDto(getTournamentFromDb(id));
    }

    /** 🟡 WRITE-THROUGH (adding golfers)
     TODO: Add @CachePut annotation: cache name - tournaments, key - #tournamentId
     **/

    @Transactional
    public GolfTournamentInfo addNewGolferToTournament(final Long tournamentId, final GolferRequest request) {
        GolfTournament tournament = getTournamentFromDb(tournamentId);
        final Golfer golfer = createGolfer(request.name());
        tournament.getGolfers().add(golfer);
        tournamentRepo.save(tournament);
        System.out.printf("💾 Write-through: added golfer '%s' to tournament %d%n", golfer.getName(), tournamentId);
        return generateTournamentDto(tournament);
    }

    /** 🔵 WRITE-BEHIND (updating hole scores)
     TODO: Add @CachePut annotation: cache name - golfers, key - #updateScoreRequest.golferId
     **/

    @Transactional(readOnly = true)
    public GolferDto updateGolferScore(final Long tournamentId, final UpdateScoreRequest updateScoreRequest) {
        GolfTournament tournament = getTournamentFromDb(tournamentId);
        Golfer golfer = tournament.getGolfers().stream()
                .filter(g -> g.getId().equals(updateScoreRequest.golferId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Golfer not found"));

        final int hole = updateScoreRequest.hole();
        final int score = updateScoreRequest.score();
        golfer.getHoleScores().put(hole, score);
        System.out.printf("🏌️ Write-behind: updated hole %d score=%d for golfer %s%n", hole, score, golfer.getName());

        writeBehindQueue.enqueue(golfer);
        return generateGolferDto(golfer);
    }

    private Golfer createGolfer(final String name){
        final Golfer golfer = new Golfer();
        golfer.setName(name);
        return golfer;
    }

    private GolfTournamentInfo generateTournamentDto(final GolfTournament tournament){
        return new GolfTournamentInfo(
                tournament.getId(),
                tournament.getName(),
                tournament.getLocation(),
                tournament.getGolfers().stream()
                        .map(g -> new GolferInfo(g.getId(), g.getName()))
                        .collect(Collectors.toSet())

        );
    }

    private GolferDto generateGolferDto(final Golfer golfer){
        return new GolferDto(
                golfer.getId(),
                golfer.getName(),
                new HashMap<>(golfer.getHoleScores()));
    }

    private GolfTournament getTournamentFromDb(Long id) {
        return tournamentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));
    }
}

