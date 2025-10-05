package org.codeus.hyperloglog.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AggregateKey {
  private String tenantId;
  private String country;
  private String device;
  private String hour;

  @Override
  public String toString() {
    return String.format("%s:%s:%s:%s", tenantId, country, device, hour);
  }
}