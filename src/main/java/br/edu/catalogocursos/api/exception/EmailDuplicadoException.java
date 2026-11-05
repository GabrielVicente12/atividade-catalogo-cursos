package br.edu.catalogocursos.api.exception;

public class EmailDuplicadoException extends RuntimeException {
    public EmailDuplicadoException() {
        super("Email já cadastrado");
    }
}
