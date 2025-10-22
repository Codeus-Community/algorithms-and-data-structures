package org.codeus.ws_deque.dto.cache;

import java.util.Set;

/**
 * @author yelyzavetalubenets
 **/
public record GolfTournamentInfo(Long id, String name, String location, Set<GolferInfo> golfers) {
}
