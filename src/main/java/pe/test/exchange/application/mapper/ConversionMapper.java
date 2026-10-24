package pe.test.exchange.application.mapper;

import org.springframework.stereotype.Component;
import pe.test.exchange.application.dto.ConversionResponse;
import pe.test.exchange.domain.model.Conversion;

import java.math.BigDecimal;

/**
 * Traduce el modelo de dominio al DTO de salida. Se escribe a mano: son 5 campos y así
 * el flujo es evidente, sin agregar una librería de mapeo solo para esto.
 */
@Component
public class ConversionMapper {

    private static final int MIN_RATE_SCALE = 2;

    public ConversionResponse toResponse(Conversion conversion) {
        return new ConversionResponse(
                conversion.getOriginalAmount(),
                conversion.getConvertedAmount(),
                conversion.getSourceCurrency(),
                conversion.getTargetCurrency(),
                presentableRate(conversion.getExchangeRate()));
    }

    private BigDecimal presentableRate(BigDecimal rate) {
        BigDecimal stripped = rate.stripTrailingZeros();
        return stripped.scale() < MIN_RATE_SCALE ? stripped.setScale(MIN_RATE_SCALE) : stripped;
    }
}
