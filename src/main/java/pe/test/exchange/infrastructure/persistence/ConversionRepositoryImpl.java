package pe.test.exchange.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.repository.ConversionRepository;

/**
 * Adaptador de persistencia: implementa el puerto del dominio con Spring Data JPA sobre H2.
 */
@Repository
@RequiredArgsConstructor
public class ConversionRepositoryImpl implements ConversionRepository {

    private final ConversionJpaRepository repository;

    @Override
    public Conversion save(Conversion conversion) {
        return repository.save(conversion);
    }
}
