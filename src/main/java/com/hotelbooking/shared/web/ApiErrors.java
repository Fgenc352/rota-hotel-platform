package com.hotelbooking.shared.web;

import com.hotelbooking.shared.exception.BusinessException;
import com.hotelbooking.shared.logging.FailureDiagnostics;
import jakarta.validation.ConstraintViolationException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;
import jakarta.persistence.LockTimeoutException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import java.util.Map;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiErrors {

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ProblemDetail> business(BusinessException e) {
        HttpStatus status = switch (e.kind()) {
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case CONFLICT -> HttpStatus.CONFLICT;
        };
        LoggerFactory.getLogger(getClass()).info("event=http.business_rejected code={} status={}", e.code(),
            status.value());
        return problem(status, e.code(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException e) {
        var response = problem(HttpStatus.BAD_REQUEST, "VALIDATION", "Form alanlarını kontrol edin.");
        var errors = e.getBindingResult().getFieldErrors().stream()
            .map(field -> Map.of(
            "field", field.getField(),
            "message", String.valueOf(field.getDefaultMessage())))
            .toList();
        response.getBody().setProperty("errors", errors);
        return response;
    }

    @ExceptionHandler({
        ConstraintViolationException.class,
        MethodArgumentTypeMismatchException.class,
        HttpMessageNotReadableException.class,
        MissingRequestHeaderException.class,
        MissingServletRequestParameterException.class
    })
    ResponseEntity<ProblemDetail> invalid(Exception e) {
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION", "İstek biçimi veya alanları geçersiz.");
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ProblemDetail> methodValidation(Exception e) {
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION", "İstek alanlarını kontrol edin.");
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    ResponseEntity<ProblemDetail> notFound(Exception e) {
        return problem(HttpStatus.NOT_FOUND, "NOT_FOUND", "İstenen adres bulunamadı.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ProblemDetail> method(Exception e) {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Bu HTTP metodu desteklenmiyor.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ProblemDetail> media(Exception e) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "Content-Type application/json olmalıdır.");
    }

    @ExceptionHandler({
        OptimisticLockingFailureException.class, PessimisticLockingFailureException.class,
        OptimisticLockException.class, PessimisticLockException.class, LockTimeoutException.class
    })
    ResponseEntity<ProblemDetail> concurrentUpdate(Exception error) {
        LoggerFactory.getLogger(getClass()).warn("event=http.persistence_conflict reason={}", error.getClass().getSimpleName());
        return problem(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION", "Kayıt eşzamanlı başka bir işlem tarafından değiştirildi. İşlemi tekrar deneyin.");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unexpected(Exception e) {
        LoggerFactory.getLogger(getClass()).error("event=http.request.failed diagnostics={}", FailureDiagnostics.describe(e));
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "İşlem tamamlanamadı. İzleme koduyla destek isteyin.");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail) {
        var body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setProperty("code", code);
        body.setProperty("traceId", String.valueOf(MDC.get("traceId")));
        return ResponseEntity.status(status).body(body);
    }
}
