package org.codeus.consistent_hashing.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitResponse {
    private String message;
    private int nodeCount;
    private int totalProducts;
    private int checkedElements;
    private Map<String, Integer> distribution;
}