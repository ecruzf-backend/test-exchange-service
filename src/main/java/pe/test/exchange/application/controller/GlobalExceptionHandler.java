package pe.test.exchange.application.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import pe.test.exchange.domain.exception.ExchangeRateUnavailableException;
import pe.test.exchange.domain.exception.InvalidAmountException;
import pe.test.exchange.domain.exception.InvalidCurrencyException;

import java.time.Instant;

/**
 * Traduce excepciones a respuestas HTTP con un formato único (RFC 9457 / application/problem+json).
 * Extiende ResponseEntityExceptionHandler para que los errores estándar de Spring MVC
 * (método no permitido, content-type inválido, etc.) usen el mismo formato.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({InvalidCurrencyException.class, InvalidAmountException.class})
    public ProblemDetail handleInvalidInput(RuntimeException ex) {
        log.warn("Solicitud inválida: {}", ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "Solicitud inválida", ex.getMessage());
    }

    @ExceptionHandler(ExchangeRateUnavailableException.class)
    public ProblemDetail handleExchangeRateUnavailable(ExchangeRateUnavailableException ex) {
        log.error("Servicio de tipo de cambio no disponible: {}", ex.getMessage(), ex);
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Servicio de tipo de cambio no disponible",
                "No se pudo obtener el tipo de cambio en este momento. Intente nuevamente más tarde.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Error inesperado", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Ocurrió un error inesperado. Intente nuevamente más tarde.");
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Solicitud inválida",
                "El cuerpo de la solicitud falta o es inválido. Se espera un JSON con "
                        + "'monto' (número), 'monedaOrigen' y 'monedaDestino' (texto).");
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
