package org.codeus.hyperloglog.aggregator.util;

public class NotImplementedException extends RuntimeException {

  public NotImplementedException(String feature) {
    super("%s is not implemented yet".formatted(feature));
  }
}
