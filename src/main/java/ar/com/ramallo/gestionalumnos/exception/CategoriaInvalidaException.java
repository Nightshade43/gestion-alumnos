package ar.com.ramallo.gestionalumnos.exception;

public class CategoriaInvalidaException extends RuntimeException {
    public CategoriaInvalidaException(String message) {
        super(message);
    }
}