package vn.hoidanit.jobhunter.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.util.matcher.RequestMatcher; // Quan trọng
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.hoidanit.jobhunter.service.RedisTokenBlacklistService;
import vn.hoidanit.jobhunter.service.UserService;

import java.io.IOException;
import java.time.Instant;

@Component
public class TokenBlacklistFilter extends OncePerRequestFilter {

    private final RedisTokenBlacklistService blacklistService;
    private final UserService userService; // Thêm UserService
    private final RequestMatcher publicApiMatcher; // Bean này sẽ được inject từ SecurityConfiguration

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistFilter.class); // Thêm logger

    public TokenBlacklistFilter(
            RedisTokenBlacklistService blacklistService,
            RequestMatcher publicApiMatcher, UserService userService) {
        this.blacklistService = blacklistService;
        this.publicApiMatcher = publicApiMatcher;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // BƯỚC 1: KIỂM TRA XEM ĐÂY CÓ PHẢI API PUBLIC KHÔNG
        // Chúng ta inject RequestMatcher từ SecurityConfig để tránh lặp lại logic
        if (publicApiMatcher.matches(request)) {
            filterChain.doFilter(request, response); // Nếu public, cho qua luôn, không kiểm tra token
            return;
        }

        // BƯỚC 2: NẾU LÀ API PRIVATE, KIỂM TRA BLACKLIST
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Kiểm tra xem user đã được xác thực (tức là có token) và token đó là JWT
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof Jwt jwt) {

            String email = jwt.getSubject();
            String jti = jwt.getClaimAsString("jti");

            // ---------- THAY ĐỔI LOGIC KIỂM TRA TẠI ĐÂY ----------

            // Check 1: JTI Blacklist (dành cho Logout/Refresh)
            if (blacklistService.isTokenBlacklisted(jti)) {
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token đã bị thu hồi (logout/refresh).");
                return;
            }

            // Check 2: Security Timestamp (dành cho đổi Role/Password)
            Long lastSecurityUpdateMillis = userService.getLastSecurityUpdateAt(email); // Lấy Long từ service
            Instant tokenIssuedAtInstant = jwt.getIssuedAt();

            if (tokenIssuedAtInstant != null && lastSecurityUpdateMillis != null) {
                long tokenIssuedAtMillis = tokenIssuedAtInstant.toEpochMilli(); // Chuyển token iat thành Long

                log.debug(">>> Checking security timestamp for {}: Token issued at (ms): {}, Last update at (ms): {}",
                        email, tokenIssuedAtMillis, lastSecurityUpdateMillis); // Thêm log debug

                // So sánh hai giá trị Long
                if (tokenIssuedAtMillis < lastSecurityUpdateMillis) {
                    log.warn(
                            ">>> Stale token detected for user: {}. Token issued at {} is before last security update at {}",
                            email, tokenIssuedAtInstant, Instant.ofEpochMilli(lastSecurityUpdateMillis)); // Log chi
                                                                                                          // tiết
                    SecurityContextHolder.clearContext();
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                            "Quyền đã thay đổi hoặc mật khẩu đã được đặt lại. Vui lòng đăng nhập lại.");
                    return;
                }
            }
        }

        // Nếu không public, và token hợp lệ (không blacklisted), cho request đi tiếp
        filterChain.doFilter(request, response);
    }
}