package pe.test.exchange.application.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pe.test.exchange.application.dto.ConversionResponse;
import pe.test.exchange.domain.model.Amount;
import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.model.CurrencyCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ConversionMapperTest {

    private final ConversionMapper mapper = new ConversionMapper();

    @Test
    void mapsAllFieldsOfTheContract() {
        Conversion conversion = conversion("100", "3.70");

        ConversionResponse response = mapper.toResponse(conversion);

        assertThat(response.monto()).isEqualByComparingTo("100");
        assertThat(response.montoConvertido()).isEqualByComparingTo("370.00");
        assertThat(response.monedaOrigen()).isEqualTo("USD");
        assertThat(response.monedaDestino()).isEqualTo("PEN");
        assertThat(response.tipoCambio()).isEqualByComparingTo("3.70");
    }

    @ParameterizedTest
    @CsvSource({
            "3.7000000000, 3.70",
            "3.7123000000, 3.7123",
            "1.0000000000, 1.00",
            "10.0000000000, 10.00",
            "0.2700000000, 0.27"
    })
    void presentsTheRateWithoutNoiseButWithAtLeastTwoDecimals(String stored, String expected) {
        ConversionResponse response = mapper.toResponse(conversion("100", stored));

        // toPlainString evita que 10.00 se imprima como 1E+1 y comprueba también la escala
        assertThat(response.tipoCambio().toPlainString()).isEqualTo(expected);
    }

    private Conversion conversion(String amount, String rate) {
        return Conversion.create(Amount.of(new BigDecimal(amount)), CurrencyCode.of("USD"),
                CurrencyCode.of("PEN"), new BigDecimal(rate), LocalDateTime.of(2026, 10, 24, 14, 0));
    }
}
