package org.codeus.ws_deque.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author yelyzavetalubenets
 **/
@Entity
@Setter @Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class GolfTournament implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String location;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<Golfer> golfers = new HashSet<>();
}
