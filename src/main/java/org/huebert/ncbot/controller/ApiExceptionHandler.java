package org.huebert.ncbot.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps service-layer validation failures ({@link IllegalArgumentException})
 * to HTTP 400 responses, keeping controllers thin. Not-found and ownership
 * violations are reported as plain-text messages.
 */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        log.debug("request rejected: {}", e.getMessage());
        return ResponseEntity.badRequest().body(e.getMessage());
    }

}
