package pe.test.exchange.domain.exception;

/**
 * El monto a convertir no cumple las reglas del negocio (nulo, no positivo, demasiados decimales o fuera de rango).
 */
public class InvalidAmountException extends DomainException {

    public InvalidAmountException(String message) {
        super(message);
    }
}
