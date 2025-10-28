package vn.hoidanit.jobhunter.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "user_sessions")
@Getter
@Setter
public class UserSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore // Quan trọng: tránh vòng lặp vô hạn khi serialize
    private User user;

    @Column(nullable = false, unique = true)
    private String refreshTokenJti; // JTI (ID) của Refresh Token

    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String userAgent;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant lastUsedAt; // Cập nhật khi refresh

    @Column(nullable = false)
    private Instant expiresAt; // Thời hạn của refresh token

    @PrePersist
    public void onPrePersist() {
        this.createdAt = Instant.now();
        this.lastUsedAt = Instant.now();
    }

    @PreUpdate
    public void onPreUpdate() {
        this.lastUsedAt = Instant.now();
    }
}