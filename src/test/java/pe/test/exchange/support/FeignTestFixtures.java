package pe.test.exchange.support;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class FeignTestFixtures {

    private FeignTestFixtures() {
    }

    public static FeignException feignError(int status) {
        return feignError(status, null);
    }

    public static FeignException feignError(int status, String body) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "https://v6.exchangerate-api.com/v6/key/latest/USD",
                Map.of(),
                null,
                StandardCharsets.UTF_8,
                new RequestTemplate());
        Response.Builder builder = Response.builder()
                .status(status)
                .reason("test")
                .request(request)
                .headers(Map.of());
        if (body != null) {
            builder.body(body, StandardCharsets.UTF_8);
        }
        return FeignException.errorStatus("ExchangeRateApiClient#getLatestRates", builder.build());
    }
}