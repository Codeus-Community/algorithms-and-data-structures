package org.codeus.hyperloglog.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Event {
  private String tenantId;
  private String userId;
  private String country;
  private String device;
  private long timestamp;
}