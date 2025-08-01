package vn.hoidanit.jobhunter.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import vn.hoidanit.jobhunter.client.ExchangeRateClient;
import vn.hoidanit.jobhunter.config.ExchangeRateProperties;
import vn.hoidanit.jobhunter.domain.response.ExchangeRateResponse;

@Service
public class ExchangeRateService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);
    private static final double FALLBACK_RATE = 25000.0; // Tỷ giá mặc định nếu API thất bại

    private final ExchangeRateClient exchangeRateClient;
    private final ExchangeRateProperties exchangeRateProperties;

    public ExchangeRateService(ExchangeRateClient exchangeRateClient, ExchangeRateProperties exchangeRateProperties) {
        this.exchangeRateClient = exchangeRateClient;
        this.exchangeRateProperties = exchangeRateProperties;
    }

    private Double fetchUsdToVndRateFromApi() {
        log.info("Đang lấy tỷ giá USD sang VND từ API");
        try {
            ExchangeRateResponse rates = exchangeRateClient.getLatestRates(
                    exchangeRateProperties.getApiKey(), "USD");
            Double vndRate = rates.getConversionRates().get("VND");
            if (vndRate != null) {
                log.info("Tỷ giá USD-VND lấy được: {}", vndRate);
                return vndRate;
            }
        } catch (Exception e) {
            log.error("Lỗi khi lấy tỷ giá USD sang VND: {}", e.getMessage());
        }
        log.warn("Không lấy được tỷ giá, sử dụng tỷ giá mặc định: {}", FALLBACK_RATE);
        return null;
    }

    @Cacheable(cacheNames = "usdToVndRate", key = "'usdToVnd'", unless = "#result == null")
    public Double getUsdToVndRate() {
        log.debug("Kiểm tra cache cho tỷ giá USD-VND");
        Double rate = fetchUsdToVndRateFromApi();
        return rate != null ? rate : FALLBACK_RATE;
    }

    @Scheduled(fixedRate = 3600000)
    @CachePut(cacheNames = "usdToVndRate", key = "'usdToVnd'")
    public Double updateUsdToVndRate() {
        log.info("Đang cập nhật tỷ giá USD sang VND");
        return getUsdToVndRate();
    }

    public double convert(double amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }

        Double usdToVndRate = getUsdToVndRate();
        log.debug("Sử dụng tỷ giá USD-VND: {} để chuyển đổi {} từ {} sang {}", usdToVndRate, amount, fromCurrency,
                toCurrency);

        if (fromCurrency.equals("VND") && toCurrency.equals("USD")) {
            return Math.round((amount / usdToVndRate) * 100.0) / 100.0;
        } else if (fromCurrency.equals("USD") && toCurrency.equals("VND")) {
            return Math.round((amount * usdToVndRate) * 100.0) / 100.0;
        }

        log.warn("Không hỗ trợ chuyển đổi từ {} sang {}, trả về số tiền gốc", fromCurrency, toCurrency);
        return amount;
    }
}