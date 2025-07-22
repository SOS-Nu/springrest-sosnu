package vn.hoidanit.jobhunter.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.hoidanit.jobhunter.domain.entity.Company;
import vn.hoidanit.jobhunter.domain.entity.User;
import vn.hoidanit.jobhunter.util.constant.UserStatusEnum;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    User findByEmail(String Email);

    boolean existsByEmail(String email);

    User findByRefreshTokenAndEmail(String token, String email);

    List<User> findByCompany(Company company);

    List<User> findByIdIn(List<Long> id);

    List<User> findAllByStatusAndIdNot(UserStatusEnum status, Long id);

    List<User> findByStatus(UserStatusEnum status);

    /**
     * Lọc sơ bộ ứng viên bằng FTS, tìm kiếm trên cả thông tin user và online
     * resume.
     * Chỉ trả về các user có isPublic = true.
     */
    @Query(value = "SELECT u.* FROM users u " +
            "LEFT JOIN online_resumes o ON u.online_resume_id = o.id " +
            // THÊM ĐIỀU KIỆN is_vip = TRUE VÀ BỌC NGOẶC CHO MATCH
            "WHERE u.is_vip = TRUE AND (" +
            "  MATCH(u.name, u.address) AGAINST(:keywords IN NATURAL LANGUAGE MODE) OR " +
            "  MATCH(o.title, o.full_name, o.summary, o.certifications, o.educations, o.languages) AGAINST(:keywords IN NATURAL LANGUAGE MODE)"
            +
            ") " + // Đóng ngoặc ở đây
            "LIMIT :limit", nativeQuery = true)
    List<User> preFilterCandidatesByKeywords(
            @Param("keywords") String keywords,
            @Param("limit") int limit);

}
