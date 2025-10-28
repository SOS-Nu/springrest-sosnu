package vn.hoidanit.jobhunter.domain.response;

import lombok.Getter;
import lombok.Setter;
import vn.hoidanit.jobhunter.domain.entity.UserSession;
import java.time.Instant;

@Getter
@Setter
public class ResSessionDTO {
    private long id;
    private String ipAddress;
    private String userAgent;
    private Instant lastUsedAt;
    private Instant createdAt;
    private boolean isCurrent; // Đánh dấu session hiện tại

    public ResSessionDTO(UserSession session, String currentJti) {
        this.id = session.getId();
        this.ipAddress = session.getIpAddress();
        this.userAgent = session.getUserAgent();
        this.lastUsedAt = session.getLastUsedAt();
        this.createdAt = session.getCreatedAt();
        this.isCurrent = (currentJti != null && currentJti.equals(session.getRefreshTokenJti()));
    }
}