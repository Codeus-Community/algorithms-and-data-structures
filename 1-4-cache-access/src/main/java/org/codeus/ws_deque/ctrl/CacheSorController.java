package org.codeus.ws_deque.ctrl;

import lombok.RequiredArgsConstructor;
import org.codeus.ws_deque.dto.cache.GolfTournamentInfo;
import org.codeus.ws_deque.dto.cache.GolferDto;
import org.codeus.ws_deque.entity.GolfTournament;
import org.codeus.ws_deque.entity.Golfer;
import org.codeus.ws_deque.service.CacheSorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * @author yelyzavetalubenets
 **/
@RestController
@RequestMapping("/data")
@RequiredArgsConstructor
public class CacheSorController {

    private final CacheSorService service;

    @GetMapping("/tournaments/fromDb")
    public List<GolfTournament> getAllTournamentsFromDatabase() {
        return service.getAllTournamentsFromDatabase();
    }

    @GetMapping("/tournaments/fromCache")
    public List<GolfTournamentInfo> getAllTournamentsFromCache() {
        return service.getAllTournamentsFromCache();
    }

    @GetMapping("/golfers/fromDb")
    public List<Golfer> getAllGolfersFromDatabase() {
        return service.getAllGolfersFromDatabase();
    }

    @GetMapping("/golfers/fromCache")
    public List<GolferDto> getAllGolfersFromCache() {
        return service.getAllGolfersFromCache();
    }

    @GetMapping("/golfers/{golferId}/fromDb")
    public Optional<Golfer> getGolferFromDb(@PathVariable Long golferId){
        return service.getGolferFromDatabase(golferId);
    }

    @GetMapping("/golfers/{golferId}/fromCache")
    public Optional<GolferDto> getGolferFromCache(@PathVariable Long golferId){
        return service.getGolferFromCache(golferId);
    }
}
