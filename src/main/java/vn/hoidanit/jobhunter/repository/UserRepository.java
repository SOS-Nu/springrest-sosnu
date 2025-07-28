package vn.hoidanit.jobhunter.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
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

        /**
         * Ghi đè phương thức findAll có sẵn để áp dụng Entity Graph.
         * Khi phương thức này được gọi trong service, nó sẽ sử dụng graph
         * 'graph.user.details'
         * để fetch tất cả các quan hệ cần thiết trong một câu query duy nhất,
         * giải quyết triệt để vấn đề N+1.
         */
        @Override
        @EntityGraph(value = "graph.user.details") // <<< PHẢI CÓ DÒNG NÀY
        Page<User> findAll(Specification<User> spec, Pageable pageable);

        // PHƯƠNG THỨC MỚI: Tìm user theo danh sách ID và fetch sẵn company, role
        @Query("SELECT u FROM User u LEFT JOIN FETCH u.company LEFT JOIN FETCH u.role WHERE u.id IN :ids")
        List<User> findByIdInWithCompanyAndRole(@Param("ids") List<Long> ids);

        // GHI ĐÈ PHƯƠNG THỨC findAllById ĐỂ ÁP DỤNG ENTITY GRAPH
        @Override
        @EntityGraph(value = "graph.user.details")
        List<User> findAllById(Iterable<Long> ids);

        // THÊM @Query VÀO ĐÂY
        @EntityGraph(value = "graph.user.details")
        @Query("SELECT u FROM User u WHERE u.id = :id")
        Optional<User> findByIdWithDetails(@Param("id") Long id);
}
