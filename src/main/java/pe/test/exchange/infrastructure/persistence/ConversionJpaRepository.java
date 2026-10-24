package pe.test.exchange.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.test.exchange.domain.model.Conversion;

/**
 * Detalle de Spring Data JPA. Solo lo usa {@link ConversionRepositoryImpl}; el resto
 * de la aplicación depende del puerto {@code domain.repository.ConversionRepository}.
 */
interface ConversionJpaRepository extends JpaRepository<Conversion, Long> {
}
