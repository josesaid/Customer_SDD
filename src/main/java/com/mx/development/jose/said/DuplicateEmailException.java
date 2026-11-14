package com.mx.development.jose.said;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("Email already in use: " + email);
    }
}
