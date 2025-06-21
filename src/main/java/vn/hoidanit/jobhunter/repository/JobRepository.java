package vn.hoidanit.jobhunter.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import vn.hoidanit.jobhunter.domain.entity.Job;
import vn.hoidanit.jobhunter.domain.entity.Skill;

@Repository
public interface JobRepository extends JpaRepository<Job, Long>,
                JpaSpecificationExecutor<Job> {

        List<Job> findBySkillsIn(List<Skill> skills);

        boolean existsByNameAndCompanyId(String name, Long companyId);

        // PHƯƠNG THỨC MỚI: Tìm các công việc còn active nhưng đã hết hạn
        List<Job> findByActiveTrueAndEndDateBefore(Instant now);
        

}
