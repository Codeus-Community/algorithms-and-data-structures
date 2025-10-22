package org.codeus.ws_deque;

import org.codeus.ws_deque.dto.GolferRequest;
import org.codeus.ws_deque.entity.GolfTournament;
import org.codeus.ws_deque.entity.Golfer;
import org.codeus.ws_deque.repo.GolfTournamentRepository;
import org.codeus.ws_deque.service.GolfTournamentService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.HashSet;
import java.util.Set;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class GolfTournamentApplication {
  public static void main(String[] args) {
    SpringApplication.run(GolfTournamentApplication.class, args);
  }



  @Bean
  CommandLineRunner init(GolfTournamentRepository repo, GolfTournamentService service) {
    return args -> {
      if (repo.count() == 0) {
        System.out.println("🏌️ Creating demo golf tournament...");

        GolfTournament tournament = new GolfTournament();
        tournament.setName("Masters Tournament");
        tournament.setLocation("Augusta National Golf Club");
        repo.save(tournament);

        Long tournamentId = tournament.getId();

        service.addNewGolferToTournament(tournamentId, new GolferRequest("Rory McIlroy"));
        service.addNewGolferToTournament(tournamentId, new GolferRequest("Phil Mickelson"));
        service.addNewGolferToTournament(tournamentId, new GolferRequest("Brooks Koepka"));
        service.addNewGolferToTournament(tournamentId, new GolferRequest("Tiger Woods"));
        service.addNewGolferToTournament(tournamentId, new GolferRequest("Dustin Johnson"));

        System.out.println("✅ Tournament initialized via service; cache populated.");
      } else {
        System.out.println("ℹ️ Tournament data already exists in DB");
      }
    };
  }
}