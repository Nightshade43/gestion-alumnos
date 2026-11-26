package ar.com.ramallo.gestionalumnos.exception;

/** Un recuperatorio que no cumple las reglas de la instancia que recupera (422). */
public class RecuperatorioInvalidoException extends RuntimeException {
    public RecuperatorioInvalidoException(String message) {
        super(message);
    }
}
