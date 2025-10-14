package org.codeus.hyperloglog.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AggregateData {
  private AggregateKey key;
  private Set<String> userIds;
}