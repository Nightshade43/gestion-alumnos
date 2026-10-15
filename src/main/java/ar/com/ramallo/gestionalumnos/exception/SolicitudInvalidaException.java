package ar.com.ramallo.gestionalumnos.exception;

/** Petición bien formada pero inconsistente entre sus campos (400). */
public class SolicitudInvalidaException extends RuntimeException {
    public SolicitudInvalidaException(String message) {
        super(message);
    }
}
