package ru.tsu.birthday.fundraiser.controller;

import feign.FeignException;
import java.util.NoSuchElementException;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({NoSuchElementException.class, FeignException.NotFound.class})
    ProblemDetail notFound(Exception exception) {
        return problem(HttpStatus.NOT_FOUND, "Not found", exception.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail forbidden(AccessDeniedException exception) {
        return problem(HttpStatus.FORBIDDEN, "Forbidden", exception.getMessage());
    }

    @ExceptionHandler({IllegalStateException.class, DataIntegrityViolationException.class,
            ObjectOptimisticLockingFailureException.class})
    ProblemDetail conflict(Exception exception) {
        var detail = exception instanceof IllegalStateException ? exception.getMessage()
                : "Fundraiser was changed by another request, try again";
        return problem(HttpStatus.CONFLICT, "Conflict", detail);
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ProblemDetail badRequest(Exception exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage());
    }

    @ExceptionHandler({FeignException.class, NoFallbackAvailableException.class})
    ProblemDetail serviceUnavailable(Exception exception) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Dependent service unavailable",
                "Gift or bank service could not complete the request");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
