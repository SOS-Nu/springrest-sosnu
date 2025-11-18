package vn.hoidanit.jobhunter.repository;

import java.util.Optional; // Dùng Optional cho phương thức mới để code an toàn hơn
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.hoidanit.jobhunter.domain.entity.Role;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long>, JpaSpecificationExecutor<Role> {

        // GIỮ NGUYÊN CÁC PHƯƠNG THỨC CỦA BẠN
        boolean existsByName(String name);

        Role findByName(String name); // Giữ nguyên, không thay đổi

        @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.id = :id")
        Optional<Role> findByIdWithPermissions(@Param("id") Long id);

        @EntityGraph(attributePaths = "permissions")
        Optional<Role> findOneWithPermissionsById(Long id);

}