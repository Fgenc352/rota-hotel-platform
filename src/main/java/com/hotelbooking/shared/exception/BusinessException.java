package com.hotelbooking.shared.exception;

public class BusinessException extends RuntimeException {
    public enum Kind {
        VALIDATION, NOT_FOUND, CONFLICT, FORBIDDEN
    }

    private final Kind kind;
    private final String code;

    public BusinessException(Kind kind, String code, String message) {
        super(message);
        this.kind = kind;
        this.code = code;
    }

    public Kind kind() {
        return kind;
    }

    public String code() {
        return code;
    }

    public static BusinessException invalid(String message) {
        return new BusinessException(Kind.VALIDATION, "VALIDATION", message);
    }

    public static BusinessException missing() {
        return new BusinessException(Kind.NOT_FOUND, "NOT_FOUND", "Kayıt bulunamadı.");
    }

    public static BusinessException forbidden(String code, String message) {
        return new BusinessException(Kind.FORBIDDEN, code, message);
    }

    public static BusinessException conflict(String code, String message) {
        return new BusinessException(Kind.CONFLICT, code, message);
    }
}
