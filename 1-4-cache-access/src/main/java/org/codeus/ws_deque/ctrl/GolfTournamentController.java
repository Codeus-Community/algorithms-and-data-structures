package org.codeus.ws_deque.ctrl;

import lombok.RequiredArgsConstructor;
import org.codeus.ws_deque.dto.GolfTournamentRequest;
import org.codeus.ws_deque.dto.GolferRequest;
import org.codeus.ws_deque.dto.UpdateScoreRequest;
import org.codeus.ws_deque.dto.cache.GolfTournamentInfo;
import org.codeus.ws_deque.dto.cache.GolferDto;
import org.codeus.ws_deque.entity.GolfTournament;
import org.codeus.ws_deque.service.GolfTournamentService;
import org.springframework.web.bind.annotation.*;

/**
 * @author yelyzavetalubenets
 **/
@RestController
@RequestMapping("/tournaments")
@RequiredArgsConstructor
public class GolfTournamentController {

    private final GolfTournamentService service;

    @PostMapping
    public GolfTournament addTournament(@RequestBody GolfTournamentRequest request){
        return service.addTournament(request);
    }

    @GetMapping("/{tournamentId}")
    public GolfTournamentInfo get(@PathVariable Long tournamentId) {
        return service.getTournament(tournamentId);
    }

    @PostMapping("/{tournamentId}/golfers")
    public GolfTournamentInfo addNewGolfer(@PathVariable Long tournamentId, @RequestBody GolferRequest request) {
        return service.addNewGolferToTournament(tournamentId, request);
    }

    @PatchMapping("/{tournamentId}/score")
    public GolferDto updateScore(@PathVariable Long tournamentId, @RequestBody UpdateScoreRequest request) {
        return service.updateGolferScore(tournamentId, request);
    }
}

