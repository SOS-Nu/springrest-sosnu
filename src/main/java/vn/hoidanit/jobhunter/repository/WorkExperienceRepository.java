package vn.hoidanit.jobhunter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.hoidanit.jobhunter.domain.entity.WorkExperience;

import java.util.List;

@Repository
public interface WorkExperienceRepository extends JpaRepository<WorkExperience, Long> {
    /**
     * Tìm tất cả kinh nghiệm làm việc của một người dùng.
     * @param userId ID của người dùng
     * @return Danh sách các WorkExperience
     */
    List<WorkExperience> findByUserId(long userId);
}
