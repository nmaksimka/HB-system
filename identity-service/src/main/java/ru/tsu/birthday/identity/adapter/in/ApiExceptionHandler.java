package ru.tsu.birthday.identity.adapter.in;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<Problem> notFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new Problem(Instant.now(), 404, ex.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ResponseEntity<Problem> badRequest(Exception ex) {
        return ResponseEntity.badRequest().body(new Problem(Instant.now(), 400, ex.getMessage()));
    }

    record Problem(Instant timestamp, int status, String detail) {}
}
