package pe.test.exchange.infrastructure.config;

import feign.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.test.exchange.domain.service.ConversionDomainService;
import pe.test.exchange.domain.service.ExchangeRateProvider;

import java.time.Clock;
import java.util.concurrent.TimeUnit;

/**
 * Ensambla las piezas del dominio (que no usan anotaciones de Spring) y define
 * los timeouts del cliente HTTP.
 */
@Configuration
public class BeanConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public ConversionDomainService conversionDomainService(ExchangeRateProvider exchangeRateProvider, Clock clock) {
        return new ConversionDomainService(exchangeRateProvider, clock);
    }

    @Bean
    public Request.Options exchangeRateRequestOptions() {
        return new Request.Options(3, TimeUnit.SECONDS, 5, TimeUnit.SECONDS, true);
    }
}
