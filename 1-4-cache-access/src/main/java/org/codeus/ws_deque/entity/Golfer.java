package org.codeus.ws_deque.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yelyzavetalubenets
 **/
@Entity
@Table(name = "golfers")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Golfer implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "golfer_scores",
            joinColumns = @JoinColumn(name = "golfer_id")
    )
    @MapKeyColumn(name = "hole_number")
    @Column(name = "score")
    private Map<Integer, Integer> holeScores = new HashMap<>();
}

