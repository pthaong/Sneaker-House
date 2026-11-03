
package com.sneakerhouse.backend.controller;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import com.sneakerhouse.backend.dto.ProfileDtos.ErrorResponse;
import com.sneakerhouse.backend.service.AuthException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(
            AuthException ex) {

        return ResponseEntity
                .status(ex.getStatus())
                .body(new ErrorResponse(
                        ex.getCode(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex) {

        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(
                        "VALIDATION_ERROR",
                        "Dữ liệu đầu vào không hợp lệ."
                ));
    }
    
@ExceptionHandler(MaxUploadSizeExceededException.class)
public ResponseEntity<ErrorResponse> handleMaxUploadSize(
        MaxUploadSizeExceededException ex) {

    return ResponseEntity
            .status(413)
            .body(new ErrorResponse(
                    "FILE_TOO_LARGE",
                    "Ảnh đại diện tối đa 5MB."
            ));
}
}
