package vn.hoidanit.jobhunter.web.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import vn.hoidanit.jobhunter.config.RateLimitProperties;

public class RateLimitingFilter extends OncePerRequestFilter {

    private final ProxyManager<String> proxyManager;
    private final BucketConfiguration config;
    private final RateLimitProperties props;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public RateLimitingFilter(ProxyManager<String> proxyManager,
            BucketConfiguration config,
            RateLimitProperties props) {
        this.proxyManager = proxyManager;
        this.config = config;
        this.props = props;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!props.isEnabled())
            return true;

        String uri = request.getRequestURI();
        // Chỉ áp dụng cho /api/**
        if (!matcher.match(props.getApplyPath(), uri))
            return true;

        // Whitelist một số đường dẫn
        List<String> whitelist = props.getWhitelist();
        for (String pattern : whitelist) {
            if (matcher.match(pattern, uri))
                return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        String key = buildKey(request);
        Supplier<BucketConfiguration> supplier = () -> config;

        Bucket bucket = proxyManager.getProxy(key, supplier);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.setHeader("X-RateLimit-Limit", String.valueOf(props.getRequestsPerSecond()));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
            return;
        }

        long waitNs = probe.getNanosToWaitForRefill();
        long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(waitNs) + 1;

        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        String body = """
                {
                  "type": "about:blank",
                  "title": "Too Many Requests",
                  "status": 429,
                  "detail": "API rate limit exceeded. Please retry later.",
                  "instance": "%s"
                }""".formatted(request.getRequestURI());

        response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
    }

    private String buildKey(HttpServletRequest request) {
        // Ưu tiên per-user nếu đã login, ngược lại per-IP (đọc từ
        // X-Forwarded-For/X-Real-IP do Nginx set)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return "user:" + auth.getName();
        }
        String ip = headerFirst(request, "X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        // Nếu X-Forwarded-For có nhiều IP thì lấy IP đầu
        int comma = ip.indexOf(',');
        if (comma > 0)
            ip = ip.substring(0, comma).trim();

        return "ip:" + ip;
    }

    private String headerFirst(HttpServletRequest req, String name) {
        String v = req.getHeader(name);
        return v != null ? v.trim() : null;
    }
}
