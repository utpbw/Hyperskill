package com.example.taskmanagement.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(Exception exception, HttpServletRequest request) {
        List<FieldError> fieldErrors;
        if (exception instanceof MethodArgumentNotValidException methodArgumentNotValidException) {
            fieldErrors = methodArgumentNotValidException.getBindingResult().getFieldErrors();
        } else if (exception instanceof BindException bindException) {
            fieldErrors = bindException.getBindingResult().getFieldErrors();
        } else {
            fieldErrors = List.of();
        }

        String message = fieldErrors.stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        if (message.isEmpty()) {
            message = exception.getMessage();
        }

        Map<String, Object> body = createBody(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), message,
                request.getRequestURI());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolations(ConstraintViolationException exception,
                                                                          HttpServletRequest request) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> formatViolationPath(violation) + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));

        Map<String, Object> body = createBody(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), message,
                request.getRequestURI());
        return ResponseEntity.badRequest().body(body);
    }

    private String formatViolationPath(ConstraintViolation<?> violation) {
        StringBuilder builder = new StringBuilder();
        boolean firstNode = true;
        for (Path.Node node : violation.getPropertyPath()) {
            if (firstNode) {
                firstNode = false;
                continue;
            }

            String name = node.getName();
            if (name == null) {
                continue;
            }

            if (name.startsWith("arg") && node.getIndex() == null && node.getKey() == null) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append('.');
            }

            builder.append(name);

            if (node.isInIterable()) {
                if (node.getIndex() != null) {
                    builder.append('[').append(node.getIndex()).append(']');
                } else if (node.getKey() != null) {
                    builder.append('[').append(node.getKey()).append(']');
                }
            }
        }

        return builder.toString();
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException exception,
                                                                              HttpServletRequest request) {
        HttpStatusCode statusCode = exception.getStatusCode();
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        String error = status != null ? status.getReasonPhrase() : "Error";
        String message = exception.getReason() != null ? exception.getReason() : "";
        Map<String, Object> body = createBody(statusCode.value(), error, message, request.getRequestURI());
        return ResponseEntity.status(statusCode).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
                                                                            HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        Map<String, Object> body = createBody(status.value(), status.getReasonPhrase(), "Malformed JSON request",
                request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    private Map<String, Object> createBody(int status, String error, String message, String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status);
        body.put("error", error);
        body.put("message", message == null ? "" : message);
        body.put("path", path);
        return body;
    }
}
