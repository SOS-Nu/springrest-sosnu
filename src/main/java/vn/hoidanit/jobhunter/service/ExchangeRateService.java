package vn.hoidanit.jobhunter.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import vn.hoidanit.jobhunter.client.ExchangeRateClient;
import vn.hoidanit.jobhunter.config.ExchangeRateProperties;
import vn.hoidanit.jobhunter.domain.response.ExchangeRateResponse;

@Service
public class ExchangeRateService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);
    private final ExchangeRateClient exchangeRateClient;
    private final ExchangeRateProperties exchangeRateProperties;

    public ExchangeRateService(ExchangeRateClient exchangeRateClient, ExchangeRateProperties exchangeRateProperties) {
        this.exchangeRateClient = exchangeRateClient;
        this.exchangeRateProperties = exchangeRateProperties;
    }

    // Annotation này sẽ tự động cache kết quả.
    // Lần gọi tiếp theo trong ngày sẽ lấy từ cache thay vì gọi API.
    @Cacheable(cacheNames = "exchangeRates", key = "#baseCurrency")
    public ExchangeRateResponse getRates(String baseCurrency) {
        log.info("Fetching exchange rates from API for base currency: {}", baseCurrency);
        return exchangeRateClient.getLatestRates(exchangeRateProperties.getApiKey(), baseCurrency);
    }

    public double convert(double amount, String fromCurrency, String toCurrency) {
        if (!fromCurrency.equals("VND")) {
            // Giả định service chỉ hỗ trợ đổi từ VND
            return amount;
        }
        try {
            ExchangeRateResponse rates = getRates(fromCurrency);
            Double rate = rates.getConversionRates().get(toCurrency.toUpperCase());
            if (rate != null) {
                return Math.round((amount * rate) * 100.0) / 100.0;
            }
        } catch (Exception e) {
            log.error("Error converting currency: {}", e.getMessage());
        }
        // Nếu có lỗi hoặc không tìm thấy tỷ giá, trả về giá trị gốc
        return amount;
    }
}