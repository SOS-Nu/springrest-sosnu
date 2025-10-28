package vn.hoidanit.jobhunter.domain.response;

import lombok.Getter;
import lombok.Setter;
import vn.hoidanit.jobhunter.domain.entity.UserSession;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class ResChangePasswordDTO {
    private ResLoginDTO newLogin; // Tokens mới cho session hiện tại
    private List<SessionInfo> otherSessions; // Các session khác

    public ResChangePasswordDTO(ResLoginDTO newLogin, List<UserSession> sessions, String currentJti) {
        this.newLogin = newLogin;
        // Lọc ra tất cả session KHÔNG PHẢI session hiện tại
        this.otherSessions = sessions.stream()
                .filter(s -> !s.getRefreshTokenJti().equals(currentJti))
                .map(SessionInfo::new)
                .collect(Collectors.toList());
    }

    @Getter
    @Setter
    public static class SessionInfo {
        private long id;
        private String ipAddress;
        private String userAgent;
        private Instant lastUsedAt;
        private Instant createdAt;

        public SessionInfo(UserSession session) {
            this.id = session.getId();
            this.ipAddress = session.getIpAddress();
            this.userAgent = session.getUserAgent();
            this.lastUsedAt = session.getLastUsedAt();
            this.createdAt = session.getCreatedAt();
        }
    }
}