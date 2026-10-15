package ar.com.ramallo.gestionalumnos.web;

import ar.com.ramallo.gestionalumnos.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String CUERPO_INVALIDO = "Cuerpo de la solicitud inválido o malformado";
    private static final int MAX_VALOR_ECHO = 50;
    // Textos de Jackson 3 (verificados contra 3.1.5 con un enum y un LocalDate inválidos): si una
    // actualización los cambia, el handler cae al mensaje genérico en vez de romper. Los tests los fijan.
    private static final Pattern CAMPO = Pattern.compile("\\[\"([^\"]+)\"\\]");
    private static final Pattern VALOR = Pattern.compile("from String \"([^\"]*)\"");
    private static final Pattern TIPO = Pattern.compile("value of type `([^`]+)`");
    private static final Pattern PERMITIDOS = Pattern.compile("accepted for Enum class: \\[([^\\]]*)\\]");
    private static final String CADENA_DE_REFERENCIA = "(through reference chain: ";

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleNoEncontrado(RecursoNoEncontradoException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(RegistroDuplicadoException.class)
    public ResponseEntity<ErrorResponse> handleDuplicado(RegistroDuplicadoException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(EstadoInvalidoException.class)
    public ResponseEntity<ErrorResponse> handleEstadoInvalido(EstadoInvalidoException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(RequisitosAcademicosIncompletosException.class)
    public ResponseEntity<ErrorResponse> handleRequisitos(RequisitosAcademicosIncompletosException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ErrorResponse> handleSolicitudInvalida(SolicitudInvalidaException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Parámetro de query o de ruta que no se puede convertir (un enum inexistente, un id no numérico).
     * Es un error del cliente: sin este handler caía en el genérico y devolvía 500.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTipoDeParametro(MethodArgumentTypeMismatchException ex) {
        String detalle = ex.getName() + ": valor inválido '" + ex.getValue() + "'";
        Class<?> requerido = ex.getRequiredType();
        if (requerido != null && requerido.isEnum()) {
            detalle += " (permitidos: " + Arrays.stream(requerido.getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", ")) + ")";
        }
        return build(HttpStatus.BAD_REQUEST, detalle);
    }

    /**
     * Falta un parámetro de query obligatorio (por ejemplo clasesAdicionales en ampliar-cupo).
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleParametroFaltante(MissingServletRequestParameterException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getParameterName() + ": es obligatorio");
    }

    /**
     * Body que Jackson no puede leer: JSON roto, enum inexistente, fecha con otro formato, texto en un número.
     * Es un error del cliente; sin este handler caía en el genérico y devolvía 500.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleCuerpoIlegible(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, describirCuerpoIlegible(ex));
    }

    /**
     * Arma "campo: valor inválido 'x' (permitidos: …)" a partir del texto de la causa de Jackson.
     * Nunca expone el mensaje crudo de la librería: si no puede identificar el campo, devuelve el genérico.
     */
    private String describirCuerpoIlegible(HttpMessageNotReadableException ex) {
        Throwable causa = ex.getCause();
        String texto = causa != null ? causa.getMessage() : null;
        if (texto == null) {
            return CUERPO_INVALIDO;
        }
        String campo = ultimoCampoDeLaCadena(texto);
        if (campo == null) {
            return CUERPO_INVALIDO;
        }
        Matcher valor = VALOR.matcher(texto);
        if (!valor.find()) {
            return campo + ": formato inválido";
        }
        String recibido = valor.group(1);
        if (recibido.length() > MAX_VALOR_ECHO) {
            recibido = recibido.substring(0, MAX_VALOR_ECHO) + "…";
        }
        String detalle = campo + ": valor inválido '" + recibido + "'";
        Matcher permitidos = PERMITIDOS.matcher(texto);
        Matcher tipo = TIPO.matcher(texto);
        if (permitidos.find()) {
            detalle += " (permitidos: " + permitidos.group(1) + ")";
        } else if (tipo.find() && tipo.group(1).equals("java.time.LocalDate")) {
            detalle += " (formato esperado: yyyy-MM-dd)";
        }
        return detalle;
    }

    /** Último nombre de propiedad de "(through reference chain: A["x"]->B["y"])": el campo que falló. */
    private String ultimoCampoDeLaCadena(String texto) {
        int inicio = texto.lastIndexOf(CADENA_DE_REFERENCIA);
        if (inicio < 0) {
            return null;
        }
        Matcher campos = CAMPO.matcher(texto.substring(inicio));
        String ultimo = null;
        while (campos.find()) {
            ultimo = campos.group(1);
        }
        return ultimo;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidacion(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, detalle);
    }

    @ExceptionHandler(CategoriaInvalidaException.class)
    public ResponseEntity<ErrorResponse> handleCategoriaInvalida(CategoriaInvalidaException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(LimiteClasesExcedidoException.class)
    public ResponseEntity<ErrorResponse> handleLimiteExcedido(LimiteClasesExcedidoException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAutenticacion(org.springframework.security.core.AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos");
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegridadDatos(org.springframework.dao.DataIntegrityViolationException ex) {
        return build(HttpStatus.CONFLICT, "El recurso viola una restricción de integridad (posible duplicado)");
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), mensaje));
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(jakarta.validation.ConstraintViolationException ex) {
        String detalle = ex.getConstraintViolations().stream()
                .map(v -> ultimoSegmento(v.getPropertyPath().toString()) + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, detalle);
    }

    private String ultimoSegmento(String path) {
        int idx = path.lastIndexOf('.');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Error no manejado", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }



}