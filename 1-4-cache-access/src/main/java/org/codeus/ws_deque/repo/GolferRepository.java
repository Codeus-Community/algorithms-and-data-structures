package org.codeus.ws_deque.repo;

import org.codeus.ws_deque.entity.Golfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author yelyzavetalubenets
 **/
@Repository
public interface GolferRepository extends JpaRepository<Golfer, Long> {}
