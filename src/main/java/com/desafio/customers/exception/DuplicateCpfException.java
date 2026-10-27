package com.desafio.customers.exception;

public class DuplicateCpfException extends RuntimeException {
    public DuplicateCpfException() {
        super("A customer with this CPF is already registered");
    }
}
