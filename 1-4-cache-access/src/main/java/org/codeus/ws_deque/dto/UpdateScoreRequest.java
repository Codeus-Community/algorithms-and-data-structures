package org.codeus.ws_deque.dto;

/**
 * @author yelyzavetalubenets
 **/
public record UpdateScoreRequest(Long golferId, int hole, int score) {
}
