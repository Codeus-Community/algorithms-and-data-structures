package org.codeus.hyperloglog.aggregator.config;

import org.codeus.hyperloglog.aggregator.util.NotImplementedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class NotImplementedExceptionHandler {

  @ExceptionHandler(NotImplementedException.class)
  @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
  public ProblemDetail handleGlobalCustomException(NotImplementedException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_IMPLEMENTED, ex.getMessage());
  }

}
