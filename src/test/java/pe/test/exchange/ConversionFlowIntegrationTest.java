package pe.test.exchange;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import pe.test.exchange.infrastructure.external.ExchangeRateApiClient;
import pe.test.exchange.infrastructure.external.dto.ExchangeRateApiResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pe.test.exchange.support.FeignTestFixtures.feignError;

/**
 * Flujo completo con el contexto real de Spring: controller -> servicio -> dominio -> adaptador Feign (mockeado
 * en el borde HTTP) -> JPA -> H2. Solo se simula el cliente Feign, para no depender de internet ni de la cuota.
 */
@SpringBootTest
class ConversionFlowIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ExchangeRateApiClient exchangeRateApiClient;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        jdbcTemplate.update("delete from conversiones");
    }

    @Test
    void convertsAndStoresTheOperation() throws Exception {
        givenRates("USD", Map.of("PEN", new BigDecimal("3.70"), "EUR", new BigDecimal("0.92")));

        mockMvc.perform(post("/api/conversion").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "monto": 100, "monedaOrigen": "USD", "monedaDestino": "PEN" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monto").value(100))
                .andExpect(jsonPath("$.montoConvertido").value(370.00))
                .andExpect(jsonPath("$.monedaOrigen").value("USD"))
                .andExpect(jsonPath("$.monedaDestino").value("PEN"))
                .andExpect(jsonPath("$.tipoCambio").value(3.70));

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("select * from conversiones");
        assertThat(rows).hasSize(1);
        Map<String, Object> row = rows.get(0);
        assertThat((BigDecimal) row.get("MONTO_ORIGINAL")).isEqualByComparingTo("100");
        assertThat(row.get("MONEDA_ORIGEN")).isEqualTo("USD");
        assertThat(row.get("MONEDA_DESTINO")).isEqualTo("PEN");
        assertThat((BigDecimal) row.get("TIPO_CAMBIO")).isEqualByComparingTo("3.70");
        assertThat((BigDecimal) row.get("MONTO_CONVERTIDO")).isEqualByComparingTo("370.00");
        assertThat(row.get("FECHA_OPERACION")).isNotNull();
        verify(exchangeRateApiClient).getLatestRates(anyString(), eq("USD"));
    }

    @Test
    void normalizesCurrencyCodesToUppercase() throws Exception {
        givenRates("USD", Map.of("PEN", new BigDecimal("3.70")));

        mockMvc.perform(post("/api/conversion").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "monto": 10, "monedaOrigen": "usd", "monedaDestino": " pen " }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monedaOrigen").value("USD"))
                .andExpect(jsonPath("$.monedaDestino").value("PEN"));
    }

    @Test
    void sameCurrencyDoesNotCallTheExternalServiceButIsStored() throws Exception {
        mockMvc.perform(post("/api/conversion").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "monto": 25.50, "monedaOrigen": "USD", "monedaDestino": "USD" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montoConvertido").value(25.50))
                .andExpect(jsonPath("$.tipoCambio").value(1.00));

        verify(exchangeRateApiClient, never()).getLatestRates(anyString(), anyString());
        assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void invalidInputIs400AndNothingIsCalledNorStored() throws Exception {
        mockMvc.perform(post("/api/conversion").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "monto": 100, "monedaOrigen": "US", "monedaDestino": "PEN" }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/conversion").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "monto": -1, "monedaOrigen": "USD", "monedaDestino": "PEN" }
                                """))
                .andExpect(status().isBadRequest());

        Mockito.verifyNoInteractions(exchangeRateApiClient);
        assertThat(countRows()).isZero();
    }

    @Test
    void unsupportedTargetCurrencyIs400AndNothingIsStored() throws Exception {
        givenRates("USD", Map.of("PEN", new BigDecimal("3.70")));

        mockMvc.perform(post("/api/conversion").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "monto": 100, "monedaOrigen": "USD", "monedaDestino": "ZZZ" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Moneda no soportada: ZZZ"));

        assertThat(countRows()).isZero();
    }

    @Test
    void externalServiceFailureIs503AndNothingIsStored() throws Exception {
        when(exchangeRateApiClient.getLatestRates(anyString(), eq("USD"))).thenThrow(feignError(503));

        mockMvc.perform(post("/api/conversion").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "monto": 100, "monedaOrigen": "USD", "monedaDestino": "PEN" }
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));

        assertThat(countRows()).isZero();
    }

    private void givenRates(String base, Map<String, BigDecimal> rates) {
        when(exchangeRateApiClient.getLatestRates(anyString(), eq(base)))
                .thenReturn(new ExchangeRateApiResponse("success", base, rates));
    }

    private int countRows() {
        Integer count = jdbcTemplate.queryForObject("select count(*) from conversiones", Integer.class);
        return count == null ? 0 : count;
    }
}
