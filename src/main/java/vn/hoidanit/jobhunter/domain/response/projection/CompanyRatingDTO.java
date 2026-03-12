package vn.hoidanit.jobhunter.domain.response.projection;

public interface CompanyRatingDTO {
    Long getCompanyId();

    Double getAverageRating();

    Long getTotalComments();
}