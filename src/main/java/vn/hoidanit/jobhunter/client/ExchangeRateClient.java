package vn.hoidanit.jobhunter.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import vn.hoidanit.jobhunter.domain.response.ExchangeRateResponse;

@FeignClient(name = "exchangeRateClient", url = "${app.exchange-rate.base-url}")
public interface ExchangeRateClient {

    @GetMapping("/{apiKey}/latest/{baseCurrency}")
    ExchangeRateResponse getLatestRates(
            @PathVariable("apiKey") String apiKey,
            @PathVariable("baseCurrency") String baseCurrency);
}