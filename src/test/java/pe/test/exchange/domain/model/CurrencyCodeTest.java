package pe.test.exchange.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import pe.test.exchange.domain.exception.InvalidCurrencyException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrencyCodeTest {

    @Test
    void normalizesToUppercaseAndTrims() {
        assertThat(CurrencyCode.of("  usd ").value()).isEqualTo("USD");
    }

    @Test
    void isEqualRegardlessOfInputCase() {
        assertThat(CurrencyCode.of("pen")).isEqualTo(CurrencyCode.of("PEN"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "US", "USDD", "U$D", "123", "ÑÑÑ"})
    void rejectsInvalidCodes(String value) {
        assertThatThrownBy(() -> CurrencyCode.of(value))
                .isInstanceOf(InvalidCurrencyException.class);
    }
}
