package vn.hoidanit.jobhunter.repository;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

  public interface CompanyJobCountDTO {
    long getCompanyId();

    long getJobCount();
  }

  /**
   * Counts active jobs for a single company.
   * 
   * @param companyId The ID of the company.
   * @return The number of active jobs.
   */
  long countByCompany_IdAndActiveTrue(long companyId);

  /**
   * Efficiently counts active jobs for a list of company IDs in a single query.
   * 
   * @param companyIds List of company IDs.
   * @return A list of DTOs containing companyId and its corresponding active job
   *         count.
   */
  @Query("SELECT j.company.id as companyId, COUNT(j.id) as jobCount " +
      "FROM Job j WHERE j.active = true AND j.company.id IN :companyIds " +
      "GROUP BY j.company.id")
  List<CompanyJobCountDTO> countActiveJobsByCompanyIds(@Param("companyIds") List<Long> companyIds);

  /**
   * Tìm kiếm các job CÓ ACTIVE bằng Full-Text Search của MySQL.
   * 
   * @param keywords Chuỗi từ khóa, ví dụ: "java spring boot"
   * @param limit    Giới hạn số lượng kết quả trả về
   * @return Danh sách các jobs phù hợp
   */
  @Query(value = "SELECT * FROM jobs " +
      "WHERE MATCH(name, description) AGAINST(:keywords IN NATURAL LANGUAGE MODE) " +
      "AND active = true " + // Chỉ tìm job active
      "LIMIT :limit", nativeQuery = true)
  List<Job> searchActiveByKeywordsNative(
      @Param("keywords") String keywords,
      @Param("limit") int limit);

  /**
   * Tìm các job CÓ ACTIVE dựa trên danh sách tên các kỹ năng.
   * 
   * @param skillNames một Set chứa tên các skills cần tìm.
   * @return Danh sách các jobs phù hợp.
   */
  @Query("SELECT j FROM Job j JOIN j.skills s WHERE j.active = true AND s.name IN :skillNames")
  List<Job> findActiveBySkillNames(@Param("skillNames") Set<String> skillNames);

  List<Job> findByCompany_Id(long companyId);

  @Override
  @EntityGraph(value = "graph.job.details")
  List<Job> findAllById(Iterable<Long> ids);

  @EntityGraph(attributePaths = { "company", "skills" })
  @Query("SELECT j FROM Job j WHERE j.active = true ORDER BY j.updatedAt DESC")
  List<Job> findAllActiveJobs(Pageable pageable);

  @Query("""
      select distinct j
      from Job j
      left join fetch j.company
      left join fetch j.skills
      where j.id in :ids
      """)
  List<Job> findAllWithCompanyAndSkillsByIdIn(@Param("ids") List<Long> ids);

}
