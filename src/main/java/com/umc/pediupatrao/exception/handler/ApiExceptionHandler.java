package com.umc.pediupatrao.exception.handler;

import com.umc.pediupatrao.exception.ConflictException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.exception.ResourceNotFoundException;
import com.umc.pediupatrao.exception.dto.StandardError;
import com.umc.pediupatrao.exception.dto.ValidationError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

// Restrito aos @RestController para não transformar em JSON os erros das telas Thymeleaf
@RestControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<StandardError> handleNotFound(ResourceNotFoundException e, HttpServletRequest request) {
        return buildStandardResponse(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<StandardError> handleConflict(ConflictException e, HttpServletRequest request) {
        return buildStandardResponse(HttpStatus.CONFLICT, e.getMessage(), request);
    }

    // Perfil sem permissão para a operação (@PreAuthorize)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<StandardError> handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
        return buildStandardResponse(HttpStatus.FORBIDDEN, "Acesso negado para o seu perfil", request);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<StandardError> handleRegraNegocio(RegraNegocioException e, HttpServletRequest request) {
        return buildStandardResponse(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    // JSON malformado ou valor de enum inexistente (ex.: tipoAtendimento = "DELIVERY")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<StandardError> handleNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        return buildStandardResponse(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationError> handleValidationError(MethodArgumentNotValidException e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
        return buildValidationResponse(status, e, request);
    }

    private ResponseEntity<StandardError> buildStandardResponse(HttpStatus status, String message, HttpServletRequest request) {
        StandardError error = new StandardError(
                Instant.now(),
                status.value(),
                "Error",
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(error);
    }

    private ResponseEntity<ValidationError> buildValidationResponse(HttpStatus status, MethodArgumentNotValidException e, HttpServletRequest request) {
        ValidationError err = new ValidationError(
                Instant.now(),
                status.value(),
                "Validation Error",
                "Dados inválidos",
                request.getRequestURI()
        );
        for (FieldError f : e.getBindingResult().getFieldErrors()) {
            err.addError(f.getField(), f.getDefaultMessage());
        }

        return ResponseEntity.status(status).body(err);
    }
}
