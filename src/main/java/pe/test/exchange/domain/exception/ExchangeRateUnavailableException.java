package pe.test.exchange.domain.exception;

/**
 * No se pudo obtener un tipo de cambio utilizable del proveedor externo
 * (servicio caído, timeout, credenciales/cuota, respuesta inválida).
 */
public class ExchangeRateUnavailableException extends DomainException {

    public ExchangeRateUnavailableException(String message) {
        super(message);
    }
}
