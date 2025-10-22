package org.codeus.ws_deque.service;

import lombok.RequiredArgsConstructor;
import org.codeus.ws_deque.dto.cache.GolfTournamentInfo;
import org.codeus.ws_deque.dto.cache.GolferDto;
import org.codeus.ws_deque.entity.GolfTournament;
import org.codeus.ws_deque.entity.Golfer;
import org.codeus.ws_deque.repo.GolfTournamentRepository;
import org.codeus.ws_deque.repo.GolferRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author yelyzavetalubenets
 **/
@Service
@RequiredArgsConstructor
public class CacheSorService {

    private final GolfTournamentRepository tournamentRepo;
    private final GolferRepository golferRepository;
    private final RedisTemplate<String, GolfTournamentInfo> redisTournamentTemplate;
    private final RedisTemplate<String, GolferDto> redisGolferTemplate;


    public List<GolfTournament> getAllTournamentsFromDatabase() {
        System.out.println("📥 Reading all tournaments from DB");
        return tournamentRepo.findAll();
    }

    public List<GolfTournamentInfo> getAllTournamentsFromCache() {
        Set<String> keys = redisTournamentTemplate.keys("tournaments*");
        if (keys == null || keys.isEmpty()) return List.of();

        List<GolfTournamentInfo> values = redisTournamentTemplate.opsForValue().multiGet(keys);
        System.out.println("⚡ Retrieved " + values.size() + " tournaments from Redis");
        return values;
    }

    public List<Golfer> getAllGolfersFromDatabase() {
        System.out.println("🏌️ Reading all golfers from DB");
        return golferRepository.findAll();
    }

    public List<GolferDto> getAllGolfersFromCache() {
        Set<String> keys = redisGolferTemplate.keys("golfers*");
        if (keys == null || keys.isEmpty()) return List.of();

        List<GolferDto> values = redisGolferTemplate.opsForValue().multiGet(keys);
        System.out.println("⚡️Retrieved " + values.size() + " golfers from Redis");
        return values;
    }

    public Optional<Golfer> getGolferFromDatabase(final Long golferId) {
        System.out.println("📥 Reading golfer ID " + golferId + " from DB");
        return golferRepository.findById(golferId);
    }

    public Optional<GolferDto> getGolferFromCache(final Long golferId) {
        String key = "golfers::" + golferId;
        GolferDto cachedGolfer = redisGolferTemplate.opsForValue().get(key);
        if (cachedGolfer != null) {
            System.out.println("⚡️ Retrieved golfer " + cachedGolfer.name() + " from Redis");
            return Optional.of(cachedGolfer);
        } else {
            System.out.println("❌ Golfer with Key " + key + " not found in cache");
            return Optional.empty();
        }
    }

}

