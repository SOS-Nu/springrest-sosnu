package vn.hoidanit.jobhunter.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.hoidanit.jobhunter.domain.entity.Comment;
import vn.hoidanit.jobhunter.domain.entity.Company;
import vn.hoidanit.jobhunter.domain.entity.User;
import vn.hoidanit.jobhunter.domain.response.projection.CompanyRatingDTO;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long>, JpaSpecificationExecutor<Comment> {

    void deleteByUserId(long userId);

    boolean existsByUserAndCompany(User user, Company company);

    @Query("SELECT c.company.id as companyId, AVG(c.rating) as averageRating, COUNT(c.id) as totalComments " +
            "FROM Comment c WHERE c.company.id IN :companyIds GROUP BY c.company.id")
    List<CompanyRatingDTO> findAverageRatingsByCompanyIds(@Param("companyIds") List<Long> companyIds);

    @Query("SELECT AVG(c.rating) FROM Comment c WHERE c.company.id = :companyId")
    Double findAverageRatingByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.company.id = :companyId")
    Long countByCompanyId(@Param("companyId") Long companyId);

}