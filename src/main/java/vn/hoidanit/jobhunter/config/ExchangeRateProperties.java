package vn.hoidanit.jobhunter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "app.exchange-rate")
@Getter
@Setter
public class ExchangeRateProperties {
    private String apiKey;
    private String baseUrl;
}