package pe.test.exchange.domain.service;

import lombok.RequiredArgsConstructor;
import pe.test.exchange.domain.exception.ExchangeRateUnavailableException;
import pe.test.exchange.domain.model.Amount;
import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.model.CurrencyCode;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Servicio de dominio: coordina la regla "convertir un monto entre dos monedas" usando el puerto
 * {@link ExchangeRateProvider}. No conoce Spring, HTTP ni JPA; se registra como bean desde
 * infrastructure.config. El {@link Clock} se inyecta para poder probar la fecha de la operación.
 */
@RequiredArgsConstructor
public class ConversionDomainService {

    private final ExchangeRateProvider exchangeRateProvider;
    private final Clock clock;

    public Conversion convert(Amount amount, CurrencyCode source, CurrencyCode target) {
        BigDecimal rate = resolveRate(source, target);
        return Conversion.create(amount, source, target, rate, LocalDateTime.now(clock));
    }

    private BigDecimal resolveRate(CurrencyCode source, CurrencyCode target) {

        if (source.equals(target)) {
            return BigDecimal.ONE;
        }
        BigDecimal rate = exchangeRateProvider.getRate(source, target);
        if (rate == null || rate.signum() <= 0) {
            throw new ExchangeRateUnavailableException(
                    "El proveedor devolvió un tipo de cambio inválido para " + source + " -> " + target);
        }
        return rate;
    }
}
