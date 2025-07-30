package vn.hoidanit.jobhunter.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
public class DashboardRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Map<String, Long> getDashboardStats() {
        Object[] result = (Object[]) entityManager.createNativeQuery("""
                    SELECT
                        (SELECT COUNT(*) FROM users),
                        (SELECT COUNT(*) FROM companies),
                        (SELECT COUNT(*) FROM jobs),
                        (SELECT COUNT(*) FROM resumes WHERE status = 'APPROVED')
                """).getSingleResult();

        Map<String, Long> map = new HashMap<>();
        map.put("totalUsers", ((Number) result[0]).longValue());
        map.put("totalCompanies", ((Number) result[1]).longValue());
        map.put("totalJobs", ((Number) result[2]).longValue());
        map.put("totalResumesApproved", ((Number) result[3]).longValue());
        return map;
    }
}
