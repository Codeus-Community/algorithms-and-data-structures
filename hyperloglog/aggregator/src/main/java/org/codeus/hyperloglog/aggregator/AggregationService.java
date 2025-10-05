package org.codeus.hyperloglog.aggregator;

import java.time.ZonedDateTime;
import java.util.Map;

public interface AggregationService {

  /**
   * Approximate unique users for the hour bucket per available event keys (as 'TenantId:CountryCode:Device:DateTime')
   *
   * @param now    timestamp used to derive the hour bucket (usually UTC now)
   * @return a collection of event key (as 'TenantId:CountryCode:Device:Hour') to approximate unique user count for the hour bucket
   */
  Map<String, Long> listUniqueUsersPerEventKeyByHour(ZonedDateTime now);

  /**
   * Approximate unique users for the hour bucket.
   *
   * @param now    timestamp used to derive the day bucket (usually UTC now)
   * @return approximate unique user count for the day bucket
   */
  long getUniqueUsersByHour(ZonedDateTime now);

  /**
   * Approximate unique users for the hour bucket for a given tenant.
   *
   * @param tenant tenant identifier (validated externally)
   * @param now    timestamp used to derive the hour bucket (usually UTC now)
   * @return approximate unique user count for the hour bucket
   */
  long getUniqueUsersByHour(String tenant, ZonedDateTime now);

  /**
   * Approximate unique users for the day bucket for a given tenant.
   *
   * @param tenant tenant identifier (validated externally)
   * @param now    timestamp used to derive the day bucket (usually UTC now)
   * @return approximate unique user count for the day bucket
   */
  long getUniqueUsersByDay(String tenant, ZonedDateTime now);
}
