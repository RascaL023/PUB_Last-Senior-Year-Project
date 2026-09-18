package id.my.rascal.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import id.my.rascal.common.ApiResponse;
import id.my.rascal.common.template.FieldErrorTemplate;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String GENERIC_UNEXPECTED =
        "An unexpected error occurred. Please try again later.";
    private static final String GENERIC_DATA_CONFLICT =
        "The request conflicts with existing data.";
    private static final String GENERIC_MALFORMED_JSON =
        "Request body is malformed or not readable JSON.";
    private static final String GENERIC_DATA_ACCESS =
        "The request could not be processed due to a data access issue.";

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<?> handleConflict(ConflictException ex) {
        return ApiResponse.error(HttpStatus.CONFLICT, 409, "CONFLICT", ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<?> handleNotFound(NotFoundException ex) {
        return ApiResponse.error(HttpStatus.NOT_FOUND, 404, "NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<?> handleBadRequest(BadRequestException ex) {
        return ApiResponse.error(HttpStatus.BAD_REQUEST, 400, "BAD_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<?> handleForbidden(ForbiddenException ex) {
        return ApiResponse.error(HttpStatus.FORBIDDEN, 403, "FORBIDDEN", ex.getMessage());
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
        return ApiResponse.error(HttpStatus.BAD_REQUEST, 400, "INVALID_ARGUMENT", ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleUnreadable(HttpMessageNotReadableException ex) {
        log.debug("Malformed JSON on {}: {}", currentUri(), ex.getMessage());
        return ApiResponse.error(HttpStatus.BAD_REQUEST, 400, "MALFORMED_JSON", GENERIC_MALFORMED_JSON);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ApiResponse.error(HttpStatus.METHOD_NOT_ALLOWED, 405, "METHOD_NOT_ALLOWED", ex.getMessage());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<?> handleUnsupportedMedia(HttpMediaTypeNotSupportedException ex) {
        return ApiResponse.error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, 415, "UNSUPPORTED_MEDIA_TYPE", ex.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<?> handleMissingParam(MissingServletRequestParameterException ex) {
        return ApiResponse.error(HttpStatus.BAD_REQUEST, 400, "MISSING_PARAMETER", ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("Data integrity violation on {}", currentUri(), ex.getMostSpecificCause());
        return ApiResponse.error(HttpStatus.CONFLICT, 409, "DUPLICATE_ENTRY", GENERIC_DATA_CONFLICT);
    }

    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<?> handleInvalidDataAccessApiUsage(InvalidDataAccessApiUsageException ex) {
        log.error("Invalid data access usage on {}", currentUri(), ex);
        return ApiResponse.error(HttpStatus.BAD_REQUEST, 400, "INVALID_DATA_ACCESS", GENERIC_DATA_ACCESS);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handleNoResource(NoResourceFoundException ex) {
        return ApiResponse.error(HttpStatus.NOT_FOUND, 404, "NOT_FOUND", "Resource not found: " + ex.getResourcePath());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleOther(Exception e) {
        log.error("Unhandled exception on {} {}", currentMethod(), currentUri(), e);
        return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, 500, "INTERNAL_SERVER_ERROR", GENERIC_UNEXPECTED);
    }

    private static String currentUri() {
        HttpServletRequest request = currentRequest();
        return request != null ? request.getRequestURI() : "unknown";
    }

    private static String currentMethod() {
        HttpServletRequest request = currentRequest();
        return request != null ? request.getMethod() : "";
    }

    private static HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }
}
