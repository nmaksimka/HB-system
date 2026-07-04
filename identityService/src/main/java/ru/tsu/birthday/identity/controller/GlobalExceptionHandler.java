package ru.tsu.birthday.identity.controller;

import jakarta.validation.ConstraintViolationException;
import java.util.NoSuchElementException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import ru.tsu.birthday.identity.service.RegistrationException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(NoSuchElementException.class)
    ProblemDetail notFound(NoSuchElementException exception) {
        return problem(HttpStatus.NOT_FOUND, "Not found", exception.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class,
            ConstraintViolationException.class, HandlerMethodValidationException.class})
    ProblemDetail badRequest(Exception exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail conflict(IllegalStateException exception) {
        return problem(HttpStatus.CONFLICT, "Conflict", exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail duplicate() {
        return problem(HttpStatus.CONFLICT, "Conflict", "The same record was created by another request");
    }

    @ExceptionHandler(RegistrationException.class)
    ProblemDetail registration(RegistrationException exception) {
        return problem(exception.status(), "Registration failed", exception.getMessage());
    }

    @ExceptionHandler(RestClientException.class)
    ProblemDetail keycloakUnavailable() {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Dependent service unavailable",
                "Authorization server could not complete the request");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
