package vn.hoidanit.jobhunter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.hoidanit.jobhunter.domain.entity.UserSession;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    // --- Các phương thức đã có ---
    Optional<UserSession> findByRefreshTokenJti(String jti);

    List<UserSession> findByUser_Id(long userId);

    @Transactional
    void deleteByRefreshTokenJti(String jti);

    @Transactional
    void deleteByExpiresAtBefore(Instant now);

    List<UserSession> findByUser_IdAndRefreshTokenJtiNot(long userId, String currentJti);

    // ========== 1. THÊM PHƯƠNG THỨC NÀY ==========
    /**
     * Tìm tất cả session của user bằng email.
     * Spring Data JPA sẽ tự tạo query JOIN sang bảng User.
     */
    List<UserSession> findByUser_Email(String email);

    // ========== 2. THÊM PHƯƠNG THỨC NÀY ==========
    /**
     * Tìm tất cả session của user bằng email, TRỪ session có JTI này.
     */
    List<UserSession> findByUser_EmailAndRefreshTokenJtiNot(String email, String currentJti);

    // Lưu ý: Nếu bạn muốn xóa trực tiếp (như tôi đề xuất trước),
    // bạn có thể dùng `deleteByUser_EmailAndRefreshTokenJtiNot`

    @Modifying
    @Transactional
    @Query("DELETE FROM UserSession s WHERE s.id IN :sessionIds AND s.user.id = :userId")
    void deleteByIdsAndUserId(@Param("sessionIds") List<Long> sessionIds, @Param("userId") long userId);

    /**
     * Xóa tất cả session của một user cụ thể MÀ đã hết hạn.
     */
    @Transactional
    @Modifying
    void deleteByUser_IdAndExpiresAtBefore(long userId, Instant now);

    /**
     * Đếm số lượng session của user cụ thể MÀ VẪN CÒN HẠN.
     */
    long countByUser_IdAndExpiresAtAfter(long userId, Instant now);

    Optional<UserSession> findFirstByUser_IdAndExpiresAtAfterOrderByCreatedAtAsc(long userId, Instant now);
}