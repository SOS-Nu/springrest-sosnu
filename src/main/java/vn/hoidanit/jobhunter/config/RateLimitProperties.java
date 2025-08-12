package vn.hoidanit.jobhunter.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.rate-limit")
public class RateLimitProperties {
    private boolean enabled = true;
    private int requestsPerSecond = 10;
    private int burstCapacity = 10;
    private String applyPath = "/api/**";
    private List<String> whitelist = List.of("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**", "/ws/**",
            "/storage/**");

    // getters/setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getRequestsPerSecond() {
        return requestsPerSecond;
    }

    public void setRequestsPerSecond(int rps) {
        this.requestsPerSecond = rps;
    }

    public int getBurstCapacity() {
        return burstCapacity;
    }

    public void setBurstCapacity(int burstCapacity) {
        this.burstCapacity = burstCapacity;
    }

    public String getApplyPath() {
        return applyPath;
    }

    public void setApplyPath(String applyPath) {
        this.applyPath = applyPath;
    }

    public List<String> getWhitelist() {
        return whitelist;
    }

    public void setWhitelist(List<String> whitelist) {
        this.whitelist = whitelist;
    }
}
