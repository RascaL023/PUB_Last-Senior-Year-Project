package id.my.rascal.common;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import id.my.rascal.common.template.*;

public class ApiResponse {

    private static final String DEFAULT_SUCCESS_MESSAGE = "Request processed successfully";
    private static final String DEFAULT_PAGED_SUCCESS_MESSAGE = "Data retrieved successfully";
    private static final String DEFAULT_VALIDATION_MESSAGE = "Validation failed";

    public static ResponseEntity<?> error(
        HttpStatus httpStatus,
        int status,
        String errorType,
        String message
    ) {
        return ResponseEntity.status(httpStatus).body(
            errorBody(status, errorType, message)
        );
    }

    public static ErrorTemplate errorBody(
        int status,
        String errorType,
        String message
    ) {
        return new ErrorTemplate(
            false,
            message,
            errorCode(status, errorType),
            null,
            MetaTemplate.now()
        );
    }

    public static ResponseEntity<?> validationError(
        List<FieldErrorTemplate> errors
    ) {
        return validationError(DEFAULT_VALIDATION_MESSAGE, errors);
    }

    public static ResponseEntity<?> validationError(
        String message,
        List<FieldErrorTemplate> errors
    ) {
        return ResponseEntity.badRequest().body(
            new ErrorTemplate(
                false,
                message,
                null,
                errors,
                MetaTemplate.now()
            )
        );
    }

    public static ResponseEntity<?> success(
        HttpStatus httpStatus,
        Object data
    ) {
        return success(httpStatus, DEFAULT_SUCCESS_MESSAGE, data);
    }

    public static ResponseEntity<?> success(
        HttpStatus httpStatus,
        String message,
        Object data
    ) {
        return ResponseEntity.status(httpStatus).body(
            new SuccessTemplate(
                true,
                message,
                data,
                MetaTemplate.now()
            )
        );
    }

    public static ResponseEntity<?> paged(
        HttpStatus httpStatus,
        String message,
        Object data,
        int currentPage,
        int perPage,
        long totalItems,
        boolean hasNext,
        boolean hasPrev
    ) {
        int totalPages = (int) Math.ceil((double) totalItems / perPage);

        MetaTemplate meta = MetaTemplate.paged(new PaginationTemplate(
            currentPage,
            perPage,
            totalItems,
            totalPages,
            hasNext,
            hasPrev
        ));

        return ResponseEntity.status(httpStatus).body(
            new SuccessPagedTemplate(
                true,
                message,
                data,
                meta
            )
        );
    }

    private static String errorCode(int status, String errorType) {
        if (errorType == null || errorType.isBlank()) {
            return String.valueOf(status);
        }

        return errorType
            .trim()
            .toUpperCase(Locale.ROOT);
    }
}
