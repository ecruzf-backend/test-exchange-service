# Desafío Técnico – API de Tipo de Cambio

Microservicio en Java 17 / Spring Boot 4 que consume **ExchangeRate-API** mediante **OpenFeign**, expone un endpoint REST para convertir montos entre monedas y **registra cada conversión en H2 (en memoria)**.

## Stack

| Tecnología | Versión |
|---|---|
| Java | 17 |
| Spring Boot | 4.0.6 |
| Spring Cloud OpenFeign | 2025.1.1 |
| Spring Data JPA + H2 | en memoria |
| Lombok | incluido |
| Maven | wrapper incluido (`mvnw`) |

## Cómo ejecutar

Requisitos: JDK 17+ y acceso a internet (para descargar dependencias y consultar ExchangeRate-API).

```bash
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

La aplicación levanta en `http://localhost:8080`.

La API key de ExchangeRate-API se lee de `application.yaml` (`exchange-rate.api.key`).

### Pruebas

```bash
./mvnw clean verify             # Windows: mvnw.cmd clean verify
```

Las pruebas **no usan internet**: el cliente Feign se simula en el borde HTTP.

## API

### `POST /api/conversion`

**Request**

```json
{
  "monto": 100,
  "monedaOrigen": "USD",
  "monedaDestino": "PEN"
}
```

**Response `200 OK`**

```json
{
  "monto": 100,
  "montoConvertido": 370.00,
  "monedaOrigen": "USD",
  "monedaDestino": "PEN",
  "tipoCambio": 3.70
}
```

> Los valores de `montoConvertido` y `tipoCambio` dependen de la tasa real del momento.

**Ejemplo con curl**

```bash
curl -X POST http://localhost:8080/api/conversion \
  -H "Content-Type: application/json" \
  -d '{"monto": 100, "monedaOrigen": "USD", "monedaDestino": "PEN"}'
```

### Reglas de negocio

- `monto`: obligatorio, mayor que 0, máximo 2 decimales y hasta 1 000 000 000 000.
- `monedaOrigen` / `monedaDestino`: código ISO 4217 de 3 letras. No distingue mayúsculas (`usd` = `USD`).
- Monto convertido: `monto × tipoCambio`, redondeado a 2 decimales (`HALF_UP`).
- Si origen y destino son la misma moneda, la tasa es `1` y **no se consulta** el servicio externo.

### Manejo de errores

Todos los errores usan el formato estándar `application/problem+json` (RFC 9457):

```json
{
  "type": "about:blank",
  "title": "Solicitud inválida",
  "status": 400,
  "detail": "Moneda no soportada: ZZZ",
  "instance": "/api/conversion",
  "timestamp": "2026-10-24T14:00:00Z"
}
```

| Situación | HTTP |
|---|---|
| Monto nulo, ≤ 0, con más de 2 decimales o fuera de rango | 400 |
| Moneda nula, con formato inválido o no soportada por el proveedor | 400 |
| JSON mal formado, sin cuerpo o `monto` no numérico | 400 |
| Método HTTP no permitido | 405 |
| ExchangeRate-API caído, con timeout, sin cuota/credenciales o con respuesta inválida | 503 |
| Error inesperado | 500 |

Las respuestas 503 y 500 devuelven un mensaje genérico; el detalle técnico queda solo en el log.

## Persistencia

Cada conversión exitosa se guarda en la tabla `conversiones`:

| Columna | Contenido |
|---|---|
| `id` | identificador autogenerado |
| `monto_original` | monto recibido |
| `moneda_origen` / `moneda_destino` | códigos ISO normalizados |
| `tipo_cambio` | tasa aplicada |
| `monto_convertido` | resultado |
| `fecha_operacion` | fecha y hora de la operación |

Consola H2: `http://localhost:8080/h2-console`
(JDBC URL `jdbc:h2:mem:testdb`, usuario `sa`, contraseña `password`)

```sql
SELECT * FROM conversiones ORDER BY id DESC;
```

Las conversiones que fallan **no** se guardan.

## Arquitectura

Se respeta la estructura de capas del reto (Domain / Application / Infrastructure). Las dependencias apuntan siempre hacia el dominio.

```
pe.test.exchange
├── domain
│   ├── model        Conversion (entidad), Amount y CurrencyCode (Value Objects)
│   ├── service      ConversionDomainService (regla de conversión) + ExchangeRateProvider (puerto)
│   ├── repository   ConversionRepository (puerto)
│   └── exception    DomainException, InvalidCurrency, InvalidAmount, ExchangeRateUnavailable
├── application
│   ├── controller   ConversionController, GlobalExceptionHandler
│   ├── service      ConversionApplicationService (caso de uso)
│   ├── dto          ConversionRequest, ConversionResponse
│   └── mapper       ConversionMapper
└── infrastructure
    ├── external     ExchangeRateApiClient (Feign) + ExchangeRateApiAdapter
    ├── persistence  ConversionRepositoryImpl (+ Spring Data JPA)
    └── config       BeanConfiguration (beans del dominio, timeouts de Feign)
```

### Decisiones de diseño

- **Puertos y adaptadores:** el dominio define *qué necesita* (`ExchangeRateProvider`, `ConversionRepository`) y la infraestructura decide *cómo*. El dominio no importa Feign ni Spring Data.
- **Value Objects con invariantes:** `Amount` y `CurrencyCode` se validan al construirse, así que un objeto inválido no puede existir. Por eso no hace falta Bean Validation.
- **`BigDecimal` en todo el flujo:** sin errores de precisión de `double`. La tasa se guarda con 10 decimales y el monto con 2.
- **Anti-corruption layer:** `ExchangeRateApiAdapter` es el único que conoce el formato de ExchangeRate-API y traduce sus fallos (404, 403/429, 5xx, timeouts, `result` distinto de `success`, moneda ausente) a excepciones de dominio.
- **Timeouts explícitos** en el cliente Feign (3 s conexión, 5 s lectura) para que un proveedor lento no deje peticiones colgadas.
- **Sin transacción alrededor de la llamada HTTP:** el caso de uso no es `@Transactional`; el guardado ya es atómico por sí mismo.
- **`Clock` inyectado** en el servicio de dominio para poder probar la fecha de la operación.
- **Errores en un solo lugar:** `GlobalExceptionHandler` traduce excepciones a HTTP; el dominio no sabe nada de HTTP.

### Pruebas incluidas

| Nivel | Qué cubre |
|---|---|
| Dominio | `Amount`, `CurrencyCode`, `Conversion`, `ConversionDomainService` (redondeo, misma moneda, tasas inválidas) |
| Aplicación | `ConversionApplicationService`, `ConversionMapper`, `ConversionController` + manejo de errores |
| Infraestructura | `ExchangeRateApiAdapter` (éxito, moneda ausente, 404, 403/429/500/503, respuesta nula o con error) |
| Integración | Flujo completo con H2: conversión + persistencia, normalización, misma moneda, entradas inválidas, fallo externo |

