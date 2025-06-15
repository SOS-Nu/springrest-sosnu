package vn.hoidanit.jobhunter.domain.response;

import lombok.Getter;
import lombok.Setter;
import vn.hoidanit.jobhunter.domain.entity.WorkExperience;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class ResWorkExperienceDTO {

    @Getter
    @Setter
    public static class WorkExperienceResponse {
        private Long id;
        private String companyName;
        private Date startDate;
        private Date endDate;
        private String description;
        private String location;
        private Instant createdAt;
        private Instant updatedAt;
    }

    public static WorkExperienceResponse convertToResponse(WorkExperience workExperience) {
        WorkExperienceResponse dto = new WorkExperienceResponse();
        dto.setId(workExperience.getId());
        dto.setCompanyName(workExperience.getCompanyName());
        dto.setStartDate(workExperience.getStartDate());
        dto.setEndDate(workExperience.getEndDate());
        dto.setDescription(workExperience.getDescription());
        dto.setLocation(workExperience.getLocation());
        dto.setCreatedAt(workExperience.getCreatedAt());
        dto.setUpdatedAt(workExperience.getUpdatedAt());
        return dto;
    }

    public static List<WorkExperienceResponse> convertToListResponse(List<WorkExperience> workExperiences) {
        return workExperiences.stream()
                .map(ResWorkExperienceDTO::convertToResponse)
                .collect(Collectors.toList());
    }
}
