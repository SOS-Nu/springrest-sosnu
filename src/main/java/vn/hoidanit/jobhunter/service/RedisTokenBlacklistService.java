package vn.hoidanit.jobhunter.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class RedisTokenBlacklistService {

    // Tiền tố (prefix) để phân biệt các loại key trong Redis
    private static final String JTI_BLACKLIST_PREFIX = "jti_blacklist:";
    private static final String USER_BLACKLIST_PREFIX = "user_blacklist:";

    private final StringRedisTemplate stringRedisTemplate;

    public RedisTokenBlacklistService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * Đưa một JTI (JWT ID) vào danh sách đen.
     * Dùng khi user logout hoặc refresh token.
     *
     * @param jti       Mã JTI của token
     * @param expiresAt Thời điểm token hết hạn
     */
    public void blacklistToken(String jti, Instant expiresAt) {
        if (jti == null || expiresAt == null) {
            return;
        }
        long remainingSeconds = getRemainingSeconds(expiresAt);
        if (remainingSeconds > 0) {
            stringRedisTemplate.opsForValue().set(
                    JTI_BLACKLIST_PREFIX + jti,
                    "blacklisted",
                    Duration.ofSeconds(remainingSeconds));
        }
    }

    /**
     * Kiểm tra xem một JTI có nằm trong danh sách đen không.
     */
    public boolean isTokenBlacklisted(String jti) {
        if (jti == null) {
            return false;
        }
        return stringRedisTemplate.hasKey(JTI_BLACKLIST_PREFIX + jti);
    }

    /**
     * Đưa email của user vào danh sách đen.
     * Dùng khi admin thay đổi quyền của user.
     *
     * @param email             Email của user
     * @param durationInSeconds Thời gian vô hiệu hóa (thường bằng thời gian hết hạn
     *                          của access token)
     */
    public void blacklistUser(String email, long durationInSeconds) {
        if (email == null || durationInSeconds <= 0) {
            return;
        }
        stringRedisTemplate.opsForValue().set(
                USER_BLACKLIST_PREFIX + email,
                "role_changed",
                Duration.ofSeconds(durationInSeconds));
    }

    /**
     * Kiểm tra xem một user có đang bị vô hiệu hóa toàn bộ token không.
     */
    public boolean isUserBlacklisted(String email) {
        if (email == null) {
            return false;
        }
        return stringRedisTemplate.hasKey(USER_BLACKLIST_PREFIX + email);
    }

    /**
     * Tính toán số giây còn lại trước khi token hết hạn.
     */
    public static long getRemainingSeconds(Instant expiresAt) {
        Instant now = Instant.now();
        if (expiresAt.isBefore(now)) {
            return 0;
        }
        return Duration.between(now, expiresAt).getSeconds();
    }
}