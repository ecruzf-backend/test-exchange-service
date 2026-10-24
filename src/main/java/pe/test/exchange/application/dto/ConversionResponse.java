package pe.test.exchange.application.dto;

import java.math.BigDecimal;

/**
 * Response del endpoint de conversión, según el contrato del reto.
 */
public record ConversionResponse(
        BigDecimal monto,
        BigDecimal montoConvertido,
        String monedaOrigen,
        String monedaDestino,
        BigDecimal tipoCambio) {
}
