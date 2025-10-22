package org.codeus.ws_deque.dto.cache;

import java.util.Map;

/**
 * @author yelyzavetalubenets
 **/
public record GolferDto(Long id, String name, Map<Integer, Integer> holeScores) {
}
