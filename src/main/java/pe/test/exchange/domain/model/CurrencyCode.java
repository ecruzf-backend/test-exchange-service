package pe.test.exchange.domain.model;

import pe.test.exchange.domain.exception.InvalidCurrencyException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Value Object: código de moneda ISO 4217 (3 letras). Siempre queda normalizado en mayúsculas,
 * por lo que "usd", " USD " y "USD" son la misma moneda.
 */
public record CurrencyCode(String value) {

    private static final Pattern ISO_4217 = Pattern.compile("[A-Z]{3}");

    public CurrencyCode {
        if (value == null || value.isBlank()) {
            throw new InvalidCurrencyException("La moneda es obligatoria");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!ISO_4217.matcher(normalized).matches()) {
            throw new InvalidCurrencyException(
                    "Código de moneda inválido: '" + value.trim() + "'. Debe tener 3 letras (ISO 4217), por ejemplo USD");
        }
        value = normalized;
    }

    public static CurrencyCode of(String value) {
        return new CurrencyCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
