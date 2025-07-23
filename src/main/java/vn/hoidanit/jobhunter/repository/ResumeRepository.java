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

    /**
     * Phương thức mới để kiểm tra xem đã tồn tại resume
     * của một user cho một job cụ thể hay chưa.
     * 
     * @param userId ID của người dùng
     * @param jobId  ID của công việc
     * @return true nếu đã tồn tại, ngược lại false
     */
    // THÊM PHƯƠNG THỨC NÀY VÀO
    boolean existsByUser_IdAndJob_Id(long userId, long jobId);

}
