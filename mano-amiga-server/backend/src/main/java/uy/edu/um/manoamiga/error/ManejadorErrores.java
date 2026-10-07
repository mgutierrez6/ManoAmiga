package uy.edu.um.manoamiga.error;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Único manejador de errores (3.6). Transforma cualquier excepción en { "error": { code, message, details? } }.
 * Los errores inesperados se loguean en el servidor y al cliente le llega un 500 genérico,
 * sin stack trace ni detalles internos.
 */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(ApiError.class)
    public ResponseEntity<Map<String, Object>> api(ApiError e) {
        return ResponseEntity.status(e.getStatus()).body(RespuestaError.cuerpo(e));
    }

    /** Body que no cumple las anotaciones (@NotBlank, @Size, @Pattern...). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> body(MethodArgumentNotValidException e) {
        List<ApiError.Detalle> detalles = new ArrayList<>();
        for (FieldError f : e.getBindingResult().getFieldErrors()) {
            detalles.add(new ApiError.Detalle(f.getField(), f.getDefaultMessage()));
        }
        e.getBindingResult().getGlobalErrors().forEach(g -> detalles.add(new ApiError.Detalle(g.getObjectName(), g.getDefaultMessage())));
        return api(ApiError.validacion(detalles));
    }

    /** Parámetros de la URL o del query string inválidos (id que no es ObjectId, page fuera de rango...). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Object>> parametros(HandlerMethodValidationException e) {
        List<ApiError.Detalle> detalles = new ArrayList<>();
        e.getParameterValidationResults().forEach(r -> {
            String campo = r.getMethodParameter().getParameterName();
            r.getResolvableErrors().forEach(err -> detalles.add(new ApiError.Detalle(
                    err instanceof FieldError f ? f.getField() : campo, err.getDefaultMessage())));
        });
        return api(ApiError.validacion(detalles));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> tipo(MethodArgumentTypeMismatchException e) {
        return api(ApiError.validacion(List.of(new ApiError.Detalle(e.getName(), "tipo inválido"))));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> faltaParametro(MissingServletRequestParameterException e) {
        return api(ApiError.validacion(List.of(new ApiError.Detalle(e.getParameterName(), "es obligatorio"))));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> faltaHeader(MissingRequestHeaderException e) {
        return api(ApiError.validacion(List.of(new ApiError.Detalle(e.getHeaderName(), "es obligatorio"))));
    }

    /** JSON mal formado, o un tipo que no corresponde (por ejemplo { "email": { "$ne": null } }: inyección NoSQL). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> json(HttpMessageNotReadableException e) {
        return api(ApiError.badRequest("JSON_INVALIDO", "El cuerpo del pedido no es JSON válido"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> mediaType(HttpMediaTypeNotSupportedException e) {
        return api(ApiError.badRequest("JSON_INVALIDO", "El cuerpo del pedido tiene que ser JSON"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> grande(MaxUploadSizeExceededException e) {
        return api(new ApiError(413, "DEMASIADO_GRANDE", "El pedido es demasiado grande"));
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class, HttpRequestMethodNotSupportedException.class})
    public ResponseEntity<Map<String, Object>> noExiste(Exception e) {
        return api(ApiError.notFound("RUTA_INEXISTENTE", "La ruta no existe"));
    }

    /** Índice único violado (email repetido, inscripción duplicada...). */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, Object>> duplicado(DuplicateKeyException e) {
        return api(ApiError.conflict("DUPLICADO", "El recurso ya existe"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> inesperado(Exception e, HttpServletRequest req) {
        log.error("Error inesperado en {} {}", req.getMethod(), req.getRequestURI(), e);
        return api(new ApiError(500, "ERROR_INTERNO", "Ocurrió un error inesperado"));
    }
}
