package pe.test.exchange.external;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.test.exchange.domain.exception.ExchangeRateUnavailableException;
import pe.test.exchange.domain.exception.InvalidCurrencyException;
import pe.test.exchange.domain.model.CurrencyCode;
import pe.test.exchange.infrastructure.external.ExchangeRateApiAdapter;
import pe.test.exchange.infrastructure.external.ExchangeRateApiClient;
import pe.test.exchange.infrastructure.external.dto.ExchangeRateApiResponse;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pe.test.exchange.support.FeignTestFixtures.feignError;

@ExtendWith(MockitoExtension.class)
class ExchangeRateApiAdapterTest {

    private static final String API_KEY = "test-key";
    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final CurrencyCode PEN = CurrencyCode.of("PEN");

    @Mock
    private ExchangeRateApiClient client;

    private ExchangeRateApiAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ExchangeRateApiAdapter(client, API_KEY);
    }

    @Test
    void returnsTheRateOfTheTargetCurrency() {
        when(client.getLatestRates(API_KEY, "USD")).thenReturn(
                response("success", Map.of("PEN", new BigDecimal("3.7123"), "EUR", new BigDecimal("0.92"))));

        BigDecimal rate = adapter.getRate(USD, PEN);

        assertThat(rate).isEqualByComparingTo("3.7123");
        verify(client).getLatestRates(API_KEY, "USD");
    }

    @Test
    void failsWithInvalidCurrencyWhenTargetIsNotInTheRates() {
        when(client.getLatestRates(API_KEY, "USD"))
                .thenReturn(response("success", Map.of("EUR", new BigDecimal("0.92"))));

        assertThatThrownBy(() -> adapter.getRate(USD, PEN))
                .isInstanceOf(InvalidCurrencyException.class)
                .hasMessageContaining("PEN");
    }

    @Test
    void failsWithInvalidCurrencyWhenRatesAreMissing() {
        when(client.getLatestRates(API_KEY, "USD")).thenReturn(response("success", null));

        assertThatThrownBy(() -> adapter.getRate(USD, PEN))
                .isInstanceOf(InvalidCurrencyException.class);
    }

    @Test
    void failsAsUnavailableWhenApiReportsAnError() {
        when(client.getLatestRates(API_KEY, "USD")).thenReturn(response("error", null));

        assertThatThrownBy(() -> adapter.getRate(USD, PEN))
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }

    @Test
    void failsAsUnavailableWhenResultIsMissing() {
        when(client.getLatestRates(API_KEY, "USD"))
                .thenReturn(response(null, Map.of("PEN", new BigDecimal("3.70"))));

        assertThatThrownBy(() -> adapter.getRate(USD, PEN))
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }

    @Test
    void failsAsUnavailableWhenResponseIsNull() {
        when(client.getLatestRates(API_KEY, "USD")).thenReturn(null);

        assertThatThrownBy(() -> adapter.getRate(USD, PEN))
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }

    @Test
    void mapsUnsupportedCodeErrorToInvalidCurrency() {
        when(client.getLatestRates(API_KEY, "USD")).thenThrow(
                feignError(404, "{\"result\":\"error\",\"error-type\":\"unsupported-code\"}"));

        assertThatThrownBy(() -> adapter.getRate(USD, PEN))
                .isInstanceOf(InvalidCurrencyException.class)
                .hasMessageContaining("USD");
    }

    @Test
    void mapsOther404ErrorsToUnavailableInsteadOfBlamingTheCurrency() {
        when(client.getLatestRates(API_KEY, "USD")).thenThrow(
                feignError(404, "{\"result\":\"error\",\"error-type\":\"invalid-key\"}"));

        assertThatThrownBy(() -> adapter.getRate(USD, PEN))
                .isInstanceOf(ExchangeRateUnavailableException.class)
                .hasMessageContaining("404");
    }

    @Test
    void mapsHttp404WithoutBodyToUnavailable() {
        when(client.getLatestRates(API_KEY, "USD")).thenThrow(feignError(404));

        assertThatThrownBy(() -> adapter.getRate(USD, PEN))
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {403, 429, 500, 503})
    void mapsOtherHttpFailuresToUnavailable(int status) {
        when(client.getLatestRates(API_KEY, "USD")).thenThrow(feignError(status));

        assertThatThrownBy(() -> adapter.getRate(USD, PEN))
                .isInstanceOf(ExchangeRateUnavailableException.class)
                .hasMessageContaining(String.valueOf(status));
    }

    private ExchangeRateApiResponse response(String result, Map<String, BigDecimal> rates) {
        return new ExchangeRateApiResponse(result, "USD", rates);
    }
}
