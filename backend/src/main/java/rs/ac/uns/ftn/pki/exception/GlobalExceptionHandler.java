package rs.ac.uns.ftn.pki.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<?> handleNotFound(NotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<?> handleBadRequest(BadRequestException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Validation failed");

        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraint(ConstraintViolationException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    private ResponseEntity<Map<String, String>> buildResponse(HttpStatus status, String message) {
        Map<String, String> body = new HashMap<>();
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(SecurityAlertException.class)
    public ResponseEntity<?> handleSecurityAlert(SecurityAlertException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }
}