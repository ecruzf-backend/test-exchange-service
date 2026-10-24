package pe.test.exchange.domain.model;

import pe.test.exchange.domain.exception.InvalidAmountException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Value Object: monto monetario a convertir. Garantiza sus propias invariantes:
 * obligatorio, mayor que cero, máximo 2 decimales y dentro de un tope razonable.
 * Usa BigDecimal para evitar errores de precisión de double/float.
 */
public record Amount(BigDecimal value) {

    public static final int SCALE = 2;
    public static final BigDecimal MAX = new BigDecimal("1000000000000"); // 1e12

    public Amount {
        if (value == null) {
            throw new InvalidAmountException("El monto es obligatorio");
        }
        if (value.signum() <= 0) {
            throw new InvalidAmountException("El monto debe ser mayor que cero");
        }
        if (value.stripTrailingZeros().scale() > SCALE) {
            throw new InvalidAmountException("El monto admite como máximo " + SCALE + " decimales");
        }
        if (value.compareTo(MAX) > 0) {
            throw new InvalidAmountException("El monto excede el máximo permitido (" + MAX.toPlainString() + ")");
        }
    }

    public static Amount of(BigDecimal value) {
        return new Amount(value);
    }

    /** Aplica un tipo de cambio y redondea a 2 decimales (HALF_UP, el redondeo comercial habitual). */
    public BigDecimal convertWith(BigDecimal rate) {
        return value.multiply(rate).setScale(SCALE, RoundingMode.HALF_UP);
    }
}
