package pe.test.exchange.domain.exception;

/**
 * La moneda es nula, no tiene formato ISO 4217 o no está soportada por el proveedor de tipo de cambio.
 */
public class InvalidCurrencyException extends DomainException {

    public InvalidCurrencyException(String message) {
        super(message);
    }
}
