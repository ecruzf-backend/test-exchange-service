package pe.test.exchange.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.test.exchange.application.dto.ConversionRequest;
import pe.test.exchange.application.dto.ConversionResponse;
import pe.test.exchange.application.mapper.ConversionMapper;
import pe.test.exchange.domain.model.Amount;
import pe.test.exchange.domain.model.Conversion;
import pe.test.exchange.domain.model.CurrencyCode;
import pe.test.exchange.domain.repository.ConversionRepository;
import pe.test.exchange.domain.service.ConversionDomainService;

/**
 * Caso de uso "convertir un monto": traduce el request a objetos de dominio, delega la regla
 * en el servicio de dominio, persiste el resultado y devuelve el DTO.
 *
 * Intencionalmente NO es @Transactional: la llamada HTTP al proveedor externo no debe mantener
 * una transacción/conexión de base de datos abierta. El guardado ya es atómico por sí mismo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversionApplicationService {

    private final ConversionDomainService conversionDomainService;
    private final ConversionRepository conversionRepository;
    private final ConversionMapper conversionMapper;

    public ConversionResponse convert(ConversionRequest request) {

        Amount amount = Amount.of(request.monto());
        CurrencyCode source = CurrencyCode.of(request.monedaOrigen());
        CurrencyCode target = CurrencyCode.of(request.monedaDestino());

        Conversion conversion = conversionDomainService.convert(amount, source, target);
        Conversion saved = conversionRepository.save(conversion);

        log.info("Conversión registrada id={} {} {} -> {} (tasa {})",
                saved.getId(), amount.value(), source, target, saved.getExchangeRate());
        return conversionMapper.toResponse(saved);
    }
}
