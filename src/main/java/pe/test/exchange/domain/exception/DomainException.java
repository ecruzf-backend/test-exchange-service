package pe.test.exchange.domain.exception;

/**
 * Raíz de las excepciones del dominio. Las capas externas (controller, handlers)
 * deciden cómo traducirlas a una respuesta; el dominio no sabe nada de HTTP.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
