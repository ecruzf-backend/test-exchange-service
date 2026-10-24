package pe.test.exchange.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.test.exchange.application.dto.ConversionRequest;
import pe.test.exchange.application.dto.ConversionResponse;
import pe.test.exchange.application.mapper.ConversionMapper;
import pe.test.exchange.domain.exception.ExchangeRateUnavailableException;
import pe.test.exchange.domain.exception.InvalidAmountException;
import pe.test.exchange.domain.exception.InvalidCurrencyException;
import pe.test.exchange.domain.model.Amount;
import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.model.CurrencyCode;
import pe.test.exchange.domain.repository.ConversionRepository;
import pe.test.exchange.domain.service.ConversionDomainService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversionApplicationServiceTest {

    private static final CurrencyCode USD = CurrencyCode.of("USD");
    private static final CurrencyCode PEN = CurrencyCode.of("PEN");

    @Mock
    private ConversionDomainService conversionDomainService;

    @Mock
    private ConversionRepository conversionRepository;

    private ConversionApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ConversionApplicationService(conversionDomainService, conversionRepository, new ConversionMapper());
    }

    @Test
    void convertsPersistsAndReturnsTheResponse() {
        Amount amount = Amount.of(new BigDecimal("100"));
        Conversion conversion = Conversion.create(amount, USD, PEN, new BigDecimal("3.70"),
                LocalDateTime.of(2026, 10, 24, 14, 0));
        when(conversionDomainService.convert(amount, USD, PEN)).thenReturn(conversion);
        when(conversionRepository.save(conversion)).thenReturn(conversion);

        ConversionResponse response = service.convert(new ConversionRequest(new BigDecimal("100"), "usd", " pen "));

        verify(conversionRepository).save(conversion);
        assertThat(response.monto()).isEqualByComparingTo("100");
        assertThat(response.montoConvertido()).isEqualByComparingTo("370.00");
        assertThat(response.monedaOrigen()).isEqualTo("USD");
        assertThat(response.monedaDestino()).isEqualTo("PEN");
        assertThat(response.tipoCambio()).isEqualByComparingTo("3.70");
    }

    @Test
    void invalidAmountNeverReachesDomainServiceNorRepository() {
        ConversionRequest request = new ConversionRequest(new BigDecimal("-5"), "USD", "PEN");

        assertThatThrownBy(() -> service.convert(request)).isInstanceOf(InvalidAmountException.class);

        verifyNoInteractions(conversionDomainService, conversionRepository);
    }

    @Test
    void invalidCurrencyNeverReachesDomainServiceNorRepository() {
        ConversionRequest request = new ConversionRequest(BigDecimal.TEN, "USD", "PE");

        assertThatThrownBy(() -> service.convert(request)).isInstanceOf(InvalidCurrencyException.class);

        verifyNoInteractions(conversionDomainService, conversionRepository);
    }

    @Test
    void doesNotPersistWhenTheRateIsUnavailable() {
        Amount amount = Amount.of(BigDecimal.TEN);
        when(conversionDomainService.convert(amount, USD, PEN))
                .thenThrow(new ExchangeRateUnavailableException("caído"));
        ConversionRequest request = new ConversionRequest(BigDecimal.TEN, "USD", "PEN");

        assertThatThrownBy(() -> service.convert(request)).isInstanceOf(ExchangeRateUnavailableException.class);

        verifyNoInteractions(conversionRepository);
    }
}
