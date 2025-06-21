package vn.hoidanit.jobhunter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import vn.hoidanit.jobhunter.domain.entity.Resume;
import vn.hoidanit.jobhunter.util.constant.ResumeStateEnum;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long>,
        JpaSpecificationExecutor<Resume> {

                    
    long countByStatus(ResumeStateEnum status);

}
