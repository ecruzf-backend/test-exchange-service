package pe.test.exchange.application.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.test.exchange.application.dto.ConversionRequest;
import pe.test.exchange.application.dto.ConversionResponse;
import pe.test.exchange.application.service.ConversionApplicationService;

/**
 * Punto de entrada REST. Solo recibe, delega y responde; no contiene lógica de negocio.
 * Los errores se traducen a HTTP en {@link GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/conversion")
@RequiredArgsConstructor
public class ConversionController {

    private final ConversionApplicationService conversionApplicationService;

    @PostMapping
    public ConversionResponse convert(@RequestBody ConversionRequest request) {
        return conversionApplicationService.convert(request);
    }
}
