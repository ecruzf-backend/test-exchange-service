package pe.test.exchange.domain.repository;

import pe.test.exchange.domain.model.Conversion;

/**
 * Puerto de salida de persistencia. El dominio solo conoce este contrato;
 * la implementación (Spring Data JPA + H2) vive en infrastructure.persistence.
 */
public interface ConversionRepository {

    Conversion save(Conversion conversion);
}
