package pe.test.exchange.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pe.test.exchange.domain.exception.InvalidAmountException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AmountTest {

    @ParameterizedTest
    @ValueSource(strings = {"100", "0.01", "10.50", "10.500", "1000000000000"})
    void acceptsValidAmounts(String value) {
        assertThat(Amount.of(new BigDecimal(value)).value()).isEqualByComparingTo(value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "-0.01", "10.005", "1000000000000.01"})
    void rejectsInvalidAmounts(String value) {
        assertThatThrownBy(() -> Amount.of(new BigDecimal(value)))
                .isInstanceOf(InvalidAmountException.class);
    }

    @Test
    void rejectsNullAmount() {
        assertThatThrownBy(() -> Amount.of(null))
                .isInstanceOf(InvalidAmountException.class)
                .hasMessageContaining("obligatorio");
    }

    @Test
    void convertsWithRateAndRoundsToTwoDecimals() {
        assertThat(Amount.of(new BigDecimal("100")).convertWith(new BigDecimal("3.70")))
                .isEqualByComparingTo("370.00");
    }

    @Test
    void roundsHalfUp() {
        // 1 * 3.705 = 3.705 -> 3.71 (HALF_UP); con HALF_EVEN daría 3.70
        assertThat(Amount.of(BigDecimal.ONE).convertWith(new BigDecimal("3.705")))
                .isEqualByComparingTo("3.71");
    }
}
