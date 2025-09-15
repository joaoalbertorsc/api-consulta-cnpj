package com.cnpjfinder.exception;

public class CnpjApiException extends RuntimeException {
    public CnpjApiException(String message) {
        super(message);
    }

    public CnpjApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
