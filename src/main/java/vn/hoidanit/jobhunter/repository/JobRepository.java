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
public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

  List<Job> findBySkillsIn(List<Skill> skills);

  boolean existsByNameAndCompanyId(String name, Long companyId);

  List<Job> findByActiveTrueAndEndDateBefore(Instant now);

  public interface CompanyJobCountDTO {
    long getCompanyId();

    long getJobCount();
  }

  long countByCompany_IdAndActiveTrue(long companyId);

  @Query("SELECT j.company.id as companyId, COUNT(j.id) as jobCount " +
      "FROM Job j WHERE j.active = true AND j.company.id IN :companyIds " +
      "GROUP BY j.company.id")
  List<CompanyJobCountDTO> countActiveJobsByCompanyIds(@Param("companyIds") List<Long> companyIds);

  @Query(value = "SELECT * FROM jobs " +
      "WHERE MATCH(name, description) AGAINST(:keywords IN NATURAL LANGUAGE MODE) " +
      "AND active = true LIMIT :limit", nativeQuery = true)
  List<Job> searchActiveByKeywordsNative(@Param("keywords") String keywords, @Param("limit") int limit);

  @Query("SELECT j FROM Job j JOIN j.skills s WHERE j.active = true AND s.name IN :skillNames")
  List<Job> findActiveBySkillNames(@Param("skillNames") Set<String> skillNames);

  List<Job> findByCompany_Id(long companyId);

  @Override
  @EntityGraph(value = "graph.job.details")
  List<Job> findAllById(Iterable<Long> ids);

  @Override
  @EntityGraph(value = "graph.job.details")
  Page<Job> findAll(Specification<Job> spec, Pageable pageable);

  @EntityGraph(attributePaths = { "company", "skills" })
  @Query("SELECT j FROM Job j WHERE j.active = true ORDER BY j.updatedAt DESC")
  List<Job> findAllActiveJobs(Pageable pageable);
}