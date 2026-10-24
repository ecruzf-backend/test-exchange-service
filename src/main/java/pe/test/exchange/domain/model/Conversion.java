package pe.test.exchange.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad (aggregate root): una conversión de moneda ya realizada.
 * Es inmutable en la práctica: se crea con {@link #create} y no expone setters.
 */
@Entity
@Table(name = "conversiones")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Conversion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "monto_original", nullable = false, precision = 30, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "moneda_origen", nullable = false, length = 3)
    private String sourceCurrency;

    @Column(name = "moneda_destino", nullable = false, length = 3)
    private String targetCurrency;

    @Column(name = "tipo_cambio", nullable = false, precision = 30, scale = 10)
    private BigDecimal exchangeRate;

    @Column(name = "monto_convertido", nullable = false, precision = 30, scale = 2)
    private BigDecimal convertedAmount;

    @Column(name = "fecha_operacion", nullable = false)
    private LocalDateTime operationDate;

    private Conversion(BigDecimal originalAmount, CurrencyCode source, CurrencyCode target,
                       BigDecimal exchangeRate, BigDecimal convertedAmount, LocalDateTime operationDate) {
        this.originalAmount = originalAmount;
        this.sourceCurrency = source.value();
        this.targetCurrency = target.value();
        this.exchangeRate = exchangeRate;
        this.convertedAmount = convertedAmount;
        this.operationDate = operationDate;
    }

    /**
     * Crea la conversión calculando el monto convertido a partir del tipo de cambio aplicado.
     * El monto original ya llega validado porque {@link Amount} protege sus invariantes.
     */
    public static Conversion create(Amount amount, CurrencyCode source, CurrencyCode target,
                                    BigDecimal exchangeRate, LocalDateTime operationDate) {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(exchangeRate, "exchangeRate");
        Objects.requireNonNull(operationDate, "operationDate");
        if (exchangeRate.signum() <= 0) {
            throw new IllegalArgumentException("El tipo de cambio debe ser mayor que cero");
        }
        return new Conversion(amount.value(), source, target, exchangeRate,
                amount.convertWith(exchangeRate), operationDate);
    }
}
