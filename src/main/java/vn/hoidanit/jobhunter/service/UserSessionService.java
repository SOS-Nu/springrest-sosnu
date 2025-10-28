package vn.hoidanit.jobhunter.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoidanit.jobhunter.domain.entity.User;
import vn.hoidanit.jobhunter.domain.entity.UserSession;
import vn.hoidanit.jobhunter.repository.UserSessionRepository;
import vn.hoidanit.jobhunter.util.error.IdInvalidException;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class UserSessionService {

    private final UserSessionRepository userSessionRepository;
    private final RedisTokenBlacklistService blacklistService;

    public UserSessionService(UserSessionRepository userSessionRepository,
            RedisTokenBlacklistService blacklistService) {
        this.userSessionRepository = userSessionRepository;
        this.blacklistService = blacklistService;
    }

    // Lấy IP từ request
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    // Lấy User-Agent
    private String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    // Tạo session mới khi login
    public UserSession createSession(User user, String refreshTokenJti, HttpServletRequest request) {
        UserSession session = new UserSession();
        session.setUser(user);
        session.setRefreshTokenJti(refreshTokenJti);
        session.setIpAddress(getIpAddress(request));
        session.setUserAgent(getUserAgent(request));
        return userSessionRepository.save(session);
    }

    // Làm mới session khi refresh token
    public UserSession refreshSession(String oldJti, String newJti) throws IdInvalidException {
        UserSession session = userSessionRepository.findByRefreshTokenJti(oldJti)
                .orElseThrow(() -> new IdInvalidException("Refresh token không hợp lệ hoặc phiên đã hết hạn"));

        session.setRefreshTokenJti(newJti);
        // lastUsedAt sẽ tự động cập nhật nhờ @PreUpdate
        return userSessionRepository.save(session);
    }

    // Xóa session khi logout
    public void deleteSession(String jti) {
        userSessionRepository.deleteByRefreshTokenJti(jti);
    }

    // Lấy tất cả session của 1 user
    @Transactional(readOnly = true)
    public List<UserSession> findSessionsByEmail(String email) {
        return userSessionRepository.findByUser_Email(email);
    }

    // Lấy tất cả session KHÁC, trừ session hiện tại
    @Transactional(readOnly = true)
    public List<UserSession> findOtherSessionsByEmail(String email, String currentJti) {
        return userSessionRepository.findByUser_EmailAndRefreshTokenJtiNot(email, currentJti);
    }

    // Đăng xuất các thiết bị khác
    public void logoutOtherSessions(String email, String currentJti, long refreshTokenDurationSeconds) {
        List<UserSession> otherSessions = findOtherSessionsByEmail(email, currentJti);

        for (UserSession session : otherSessions) {
            // 1. Blacklist JTI của refresh token
            // Thời gian hết hạn là thời gian của refresh token (ví dụ 30 ngày)
            Instant expiry = Instant.now().plusSeconds(refreshTokenDurationSeconds);
            blacklistService.blacklistToken(session.getRefreshTokenJti(), expiry);

            // 2. Xóa session khỏi DB
            userSessionRepository.delete(session);
        }
    }
}