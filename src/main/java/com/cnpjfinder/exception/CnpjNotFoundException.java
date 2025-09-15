package com.cnpjfinder.exception;

public class CnpjNotFoundException extends CnpjApiException {
    public CnpjNotFoundException(String cnpj) {
        super("CNPJ não encontrado: " + cnpj);
    }
}
