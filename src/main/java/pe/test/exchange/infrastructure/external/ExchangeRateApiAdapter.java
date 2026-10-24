package pe.test.exchange.infrastructure.external;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.test.exchange.domain.exception.ExchangeRateUnavailableException;
import pe.test.exchange.domain.exception.InvalidCurrencyException;
import pe.test.exchange.domain.model.CurrencyCode;
import pe.test.exchange.domain.service.ExchangeRateProvider;
import pe.test.exchange.infrastructure.external.dto.ExchangeRateApiResponse;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Adaptador del puerto {@link ExchangeRateProvider} sobre el cliente Feign de ExchangeRate-API.
 * Es el único lugar que conoce el formato de la API externa y traduce sus fallos
 * (HTTP 4xx/5xx, timeouts, cuerpo con result=error, moneda ausente) a excepciones de dominio.
 */
@Slf4j
@Component
public class ExchangeRateApiAdapter implements ExchangeRateProvider {

    private static final int HTTP_NOT_FOUND = 404;
    private static final String RESULT_SUCCESS = "success";
    private static final String ERROR_UNSUPPORTED_CODE = "unsupported-code";

    private final ExchangeRateApiClient client;
    private final String apiKey;

    public ExchangeRateApiAdapter(ExchangeRateApiClient client,
                                  @Value("${exchange-rate.api.key}") String apiKey) {
        this.client = client;
        this.apiKey = apiKey;
    }

    @Override
    public BigDecimal getRate(CurrencyCode source, CurrencyCode target) {

        ExchangeRateApiResponse response = Optional.ofNullable(fetchLatestRates(source))
                .filter(r -> RESULT_SUCCESS.equalsIgnoreCase(r.result()))
                .orElseThrow(() -> new ExchangeRateUnavailableException(
                        "ExchangeRate-API respondió sin éxito para la base " + source));

        return Optional.ofNullable(response.conversionRates())
                .map(rates -> rates.get(target.value()))
                .orElseThrow(() -> new InvalidCurrencyException("Moneda no soportada: " + target));
    }

    private ExchangeRateApiResponse fetchLatestRates(CurrencyCode source) {
        try {
            return client.getLatestRates(apiKey, source.value());
        } catch (FeignException ex) {
            String body = ex.contentUTF8();
            if (ex.status() == HTTP_NOT_FOUND && body.contains(ERROR_UNSUPPORTED_CODE)) {
                throw new InvalidCurrencyException("Moneda no soportada: " + source);
            }

            log.error("ExchangeRate-API respondió HTTP {} para la base {}: {}", ex.status(), source, body);
            throw new ExchangeRateUnavailableException(
                    "Falló la consulta a ExchangeRate-API (HTTP " + ex.status() + ")");
        }
    }
}
