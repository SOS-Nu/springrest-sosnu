package vn.hoidanit.jobhunter.repository;

import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.hoidanit.jobhunter.domain.entity.Company;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long>, JpaSpecificationExecutor<Company> {
    @Query("SELECT COUNT(j) FROM Job j WHERE j.company.id = :companyId AND j.active = true")
    long countActiveJobsByCompanyId(@Param("companyId") Long companyId);

    @Query(value = """
            SELECT c.* FROM companies c
            LEFT JOIN users u ON c.id = u.company_id
            WHERE c.id = :companyId
            ORDER BY u.id ASC
            LIMIT 1
            """, nativeQuery = true)
    Optional<Company> findByIdWithHrUser(@Param("companyId") Long companyId);

}
