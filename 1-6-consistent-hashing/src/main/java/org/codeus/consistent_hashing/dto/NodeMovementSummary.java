package org.codeus.consistent_hashing.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeMovementSummary {
    private String fromNode;
    private String toNode;
    private int count;
}