package pe.test.exchange.domain.service;

import pe.test.exchange.domain.model.CurrencyCode;

import java.math.BigDecimal;

/**
 * Puerto de salida hacia el proveedor de tipos de cambio. El dominio define QUÉ necesita
 * (una tasa) y la infraestructura decide CÓMO obtenerla (Feign + ExchangeRate-API).
 */
public interface ExchangeRateProvider {

    /**
     * @return cuántas unidades de {@code target} equivalen a 1 unidad de {@code source}
     * @throws pe.test.exchange.domain.exception.InvalidCurrencyException        si alguna moneda no está soportada
     * @throws pe.test.exchange.domain.exception.ExchangeRateUnavailableException si no se puede obtener la tasa
     */
    BigDecimal getRate(CurrencyCode source, CurrencyCode target);
}
