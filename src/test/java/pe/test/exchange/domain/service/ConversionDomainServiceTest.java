package pe.test.exchange.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.test.exchange.domain.exception.ExchangeRateUnavailableException;
import pe.test.exchange.domain.exception.InvalidCurrencyException;
import pe.test.exchange.domain.model.Amount;
import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.model.CurrencyCode;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversionDomainServiceTest {

    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final CurrencyCode PEN = CurrencyCode.of("PEN");
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-10-24T14:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ExchangeRateProvider exchangeRateProvider;

    private ConversionDomainService service;

    @BeforeEach
    void setUp() {
        service = new ConversionDomainService(exchangeRateProvider, FIXED_CLOCK);
    }

    @Test
    void convertsUsingTheProviderRate() {
        when(exchangeRateProvider.getRate(USD, PEN)).thenReturn(new BigDecimal("3.70"));

        Conversion conversion = service.convert(Amount.of(new BigDecimal("100")), USD, PEN);

        assertThat(conversion.getOriginalAmount()).isEqualByComparingTo("100");
        assertThat(conversion.getConvertedAmount()).isEqualByComparingTo("370.00");
        assertThat(conversion.getExchangeRate()).isEqualByComparingTo("3.70");
        assertThat(conversion.getSourceCurrency()).isEqualTo("USD");
        assertThat(conversion.getTargetCurrency()).isEqualTo("PEN");
        assertThat(conversion.getOperationDate()).isEqualTo(LocalDateTime.of(2026, 10, 24, 14, 0));
    }

    @Test
    void roundsConvertedAmountHalfUp() {
        when(exchangeRateProvider.getRate(USD, PEN)).thenReturn(new BigDecimal("3.705"));

        Conversion conversion = service.convert(Amount.of(BigDecimal.ONE), USD, PEN);

        assertThat(conversion.getConvertedAmount()).isEqualByComparingTo("3.71");
    }

    @Test
    void sameCurrencyUsesRateOneWithoutCallingTheProvider() {
        Conversion conversion = service.convert(Amount.of(new BigDecimal("50.25")), USD, USD);

        assertThat(conversion.getExchangeRate()).isEqualByComparingTo("1");
        assertThat(conversion.getConvertedAmount()).isEqualByComparingTo("50.25");
        verifyNoInteractions(exchangeRateProvider);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-3.7"})
    void rejectsNonPositiveRateFromProvider(String rate) {
        when(exchangeRateProvider.getRate(USD, PEN)).thenReturn(new BigDecimal(rate));
        Amount amount = Amount.of(BigDecimal.TEN);

        assertThatThrownBy(() -> service.convert(amount, USD, PEN))
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }

    @Test
    void rejectsNullRateFromProvider() {
        when(exchangeRateProvider.getRate(USD, PEN)).thenReturn(null);
        Amount amount = Amount.of(BigDecimal.TEN);

        assertThatThrownBy(() -> service.convert(amount, USD, PEN))
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }

    @Test
    void propagatesProviderFailures() {
        when(exchangeRateProvider.getRate(USD, PEN))
                .thenThrow(new ExchangeRateUnavailableException("servicio caído"));
        Amount amount = Amount.of(BigDecimal.TEN);

        assertThatThrownBy(() -> service.convert(amount, USD, PEN))
                .isInstanceOf(ExchangeRateUnavailableException.class)
                .hasMessage("servicio caído");
    }

    @Test
    void propagatesUnsupportedCurrency() {
        when(exchangeRateProvider.getRate(USD, PEN))
                .thenThrow(new InvalidCurrencyException("Moneda no soportada: PEN"));
        Amount amount = Amount.of(BigDecimal.TEN);

        assertThatThrownBy(() -> service.convert(amount, USD, PEN))
                .isInstanceOf(InvalidCurrencyException.class);
    }
}
