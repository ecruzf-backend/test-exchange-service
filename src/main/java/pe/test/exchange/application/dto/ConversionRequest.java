package pe.test.exchange.application.dto;

import java.math.BigDecimal;

/**
 * Request del endpoint de conversión. Los nombres de los campos siguen el contrato del reto.
 * Las reglas de validación (monto positivo, moneda ISO 4217, etc.) viven en los Value Objects del dominio.
 */
public record ConversionRequest(
        BigDecimal monto,
        String monedaOrigen,
        String monedaDestino) {
}
