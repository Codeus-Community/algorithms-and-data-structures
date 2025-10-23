package org.codeus.consistent_hashing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoveNodeResponse {
    private String message;
    private int nodeCount;
    private int checkedElements;
    private int movedElements;
    private Map<String, Integer> distribution;
    private List<NodeMovementSummary> movements;
}