package pe.test.exchange.application.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.test.exchange.application.dto.ConversionRequest;
import pe.test.exchange.application.dto.ConversionResponse;
import pe.test.exchange.application.service.ConversionApplicationService;
import pe.test.exchange.domain.exception.ExchangeRateUnavailableException;
import pe.test.exchange.domain.exception.InvalidCurrencyException;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de la capa web sin levantar Spring: controller + GlobalExceptionHandler sobre MockMvc standalone.
 */
@ExtendWith(MockitoExtension.class)
class ConversionControllerTest {

    private static final String URL = "/api/conversion";
    private static final String VALID_BODY = """
            { "monto": 100, "monedaOrigen": "USD", "monedaDestino": "PEN" }
            """;

    @Mock
    private ConversionApplicationService conversionApplicationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ConversionController(conversionApplicationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsTheConversionWithTheContractFields() throws Exception {
        when(conversionApplicationService.convert(any(ConversionRequest.class))).thenReturn(
                new ConversionResponse(new BigDecimal("100"), new BigDecimal("370.00"), "USD", "PEN",
                        new BigDecimal("3.70")));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monto").value(100))
                .andExpect(jsonPath("$.montoConvertido").value(370.00))
                .andExpect(jsonPath("$.monedaOrigen").value("USD"))
                .andExpect(jsonPath("$.monedaDestino").value("PEN"))
                .andExpect(jsonPath("$.tipoCambio").value(3.70));
    }

    @Test
    void domainValidationErrorsAre400() throws Exception {
        when(conversionApplicationService.convert(any(ConversionRequest.class)))
                .thenThrow(new InvalidCurrencyException("Moneda no soportada: XXX"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Solicitud inválida"))
                .andExpect(jsonPath("$.detail").value("Moneda no soportada: XXX"));
    }

    @Test
    void unavailableProviderIs503WithoutLeakingInternals() throws Exception {
        when(conversionApplicationService.convert(any(ConversionRequest.class)))
                .thenThrow(new ExchangeRateUnavailableException("Falló la consulta a ExchangeRate-API (HTTP 503)"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.detail", not(containsString("ExchangeRate-API"))));
    }

    @Test
    void unexpectedErrorsAre500WithGenericMessage() throws Exception {
        when(conversionApplicationService.convert(any(ConversionRequest.class)))
                .thenThrow(new IllegalStateException("detalle interno"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail", not(containsString("detalle interno"))));
    }

    @Test
    void malformedJsonIs400() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{ monto: "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Solicitud inválida"));

        verifyNoInteractions(conversionApplicationService);
    }

    @Test
    void nonNumericAmountIs400() throws Exception {
        String body = """
                { "monto": "abc", "monedaOrigen": "USD", "monedaDestino": "PEN" }
                """;

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(conversionApplicationService);
    }

    @Test
    void missingBodyIs400() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(conversionApplicationService);
    }

    @Test
    void wrongHttpMethodIs405() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isMethodNotAllowed());
    }
}
