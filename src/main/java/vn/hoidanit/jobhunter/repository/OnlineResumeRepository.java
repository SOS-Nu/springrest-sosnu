package vn.hoidanit.jobhunter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import vn.hoidanit.jobhunter.domain.entity.OnlineResume;

import java.util.Optional;

@Repository
public interface OnlineResumeRepository extends JpaRepository<OnlineResume, Long>, JpaSpecificationExecutor<OnlineResume> {
    /**
     * Tìm kiếm OnlineResume dựa trên user id.
     * Vì mối quan hệ là 1-1, nên sẽ chỉ có tối đa một kết quả.
     *
     * @param userId Id của user
     * @return Optional chứa OnlineResume nếu tìm thấy
     */
    Optional<OnlineResume> findByUserId(long userId);
}
