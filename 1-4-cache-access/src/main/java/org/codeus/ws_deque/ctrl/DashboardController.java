package org.codeus.ws_deque.ctrl;

import lombok.RequiredArgsConstructor;
import org.codeus.ws_deque.dto.cache.GolfTournamentInfo;
import org.codeus.ws_deque.dto.cache.GolferDto;
import org.codeus.ws_deque.entity.GolfTournament;
import org.codeus.ws_deque.service.CacheSorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author yelyzavetalubenets
 **/
@Controller
@RequiredArgsConstructor
public class DashboardController {
    private final CacheSorService cacheService;

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        List<GolfTournament> dbTournaments = cacheService.getAllTournamentsFromDatabase();
        List<GolfTournamentInfo> cacheTournaments = cacheService.getAllTournamentsFromCache();
        List<GolferDto> cacheGolfers = cacheService.getAllGolfersFromCache();

        model.addAttribute("dbTournaments", dbTournaments);
        model.addAttribute("cacheTournaments", cacheTournaments);
        model.addAttribute("cacheGolfers", cacheGolfers);
        return "dashboard";
    }
}
