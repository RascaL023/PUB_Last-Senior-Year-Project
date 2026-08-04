package id.my.rascal.common.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import id.my.rascal.common.ApiResponse;
import id.my.rascal.common.template.FieldErrorTemplate;

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    // TODO: Masking error message
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<?> handleConflict(ConflictException ex) {
        return ApiResponse.error(HttpStatus.CONFLICT, 409, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<?> handleNotFound(NotFoundException ex) {
        return ApiResponse.error(HttpStatus.NOT_FOUND, 404, "Not Found", ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<?> handleBadRequest(BadRequestException ex) {
        return ApiResponse.error(HttpStatus.BAD_REQUEST, 400, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        var errorDetails = ex.getBindingResult().getFieldErrors()
            .stream()
            .map(field -> new FieldErrorTemplate(field.getField(), field.getDefaultMessage()))
            .toList();
        return ApiResponse.validationError(errorDetails);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
        return ApiResponse.error(HttpStatus.BAD_REQUEST, 400, "Invalid Argument", ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleUnreadable(HttpMessageNotReadableException ex) {
        return ApiResponse.error(HttpStatus.BAD_REQUEST, 400, "Malformed JSON", ex.getMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ApiResponse.error(HttpStatus.METHOD_NOT_ALLOWED, 405, "Method Not Allowed", ex.getMessage());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<?> handleUnsupportedMedia(HttpMediaTypeNotSupportedException ex) {
        return ApiResponse.error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, 415, "Unsupported Media Type", ex.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<?> handleMissingParam(MissingServletRequestParameterException ex) {
        return ApiResponse.error(HttpStatus.BAD_REQUEST, 400, "Missing Parameter", ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return ApiResponse.error(HttpStatus.CONFLICT, 409, "Duplicate entry", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleOther(Exception e) {
        return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, 500, "Internal Server Error", e.getMessage());
    }
}
