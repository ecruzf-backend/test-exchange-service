package pe.test.exchange.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConversionTest {

    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final CurrencyCode PEN = CurrencyCode.of("PEN");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 24, 14, 0);

    @Test
    void createCalculatesConvertedAmountFromRate() {
        Conversion conversion = Conversion.create(
                Amount.of(new BigDecimal("100")), USD, PEN, new BigDecimal("3.70"), NOW);

        assertThat(conversion.getId()).isNull(); // aún no persistida
        assertThat(conversion.getOriginalAmount()).isEqualByComparingTo("100");
        assertThat(conversion.getSourceCurrency()).isEqualTo("USD");
        assertThat(conversion.getTargetCurrency()).isEqualTo("PEN");
        assertThat(conversion.getExchangeRate()).isEqualByComparingTo("3.70");
        assertThat(conversion.getConvertedAmount()).isEqualByComparingTo("370.00");
        assertThat(conversion.getOperationDate()).isEqualTo(NOW);
    }

    @Test
    void createRejectsNonPositiveRate() {
        Amount amount = Amount.of(BigDecimal.TEN);

        assertThatThrownBy(() -> Conversion.create(amount, USD, PEN, BigDecimal.ZERO, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
