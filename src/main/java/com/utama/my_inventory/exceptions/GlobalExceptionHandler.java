package com.utama.my_inventory.exceptions;

import com.utama.my_inventory.dtos.BaseResponse;
import com.utama.my_inventory.dtos.ExtendedBaseResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ExtendedBaseResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        ExtendedBaseResponse<Void> response = ExtendedBaseResponse.<Void>of(
                BaseResponse.notFound(ex.getMessage()),
                null
        );
        return response.toResponseEntity();
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ExtendedBaseResponse<Void>> handleBusinessException(BusinessException ex) {
        ExtendedBaseResponse<Void> response = ExtendedBaseResponse.<Void>of(
                BaseResponse.conflict(ex.getMessage()),
                null
        );
        return response.toResponseEntity();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExtendedBaseResponse<Void>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ExtendedBaseResponse<Void> response = ExtendedBaseResponse.<Void>of(
                BaseResponse.badRequest("Error de validación: " + errorMessage),
                null
        );
        return response.toResponseEntity();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExtendedBaseResponse<Void>> handleGenericException(Exception ex) {
        ExtendedBaseResponse<Void> response = ExtendedBaseResponse.<Void>of(
                BaseResponse.internalError("Error interno del servidor: " + ex.getMessage()),
                null
        );
        return response.toResponseEntity();
    }
}