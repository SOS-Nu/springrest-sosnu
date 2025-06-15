package vn.hoidanit.jobhunter.domain.response;

import lombok.Getter;
import lombok.Setter;
import vn.hoidanit.jobhunter.domain.entity.OnlineResume;
import vn.hoidanit.jobhunter.domain.entity.Skill;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class ResOnlineResumeDTO {

    @Getter
    @Setter
    private abstract static class BaseResumeDTO {
        private Long id;
        private String title;
        private String fullName;
        private String email;
        private String phone;
        private String address;
        private String summary;
        private String certifications;
        private String educations;
        private String languages;
        private List<SkillInfo> skills;
        private UserInfo user;
    }

    @Getter
    @Setter
    public static class ResCreateOnlineResumeDTO extends BaseResumeDTO {
        private Instant createdAt;
    }

    @Getter
    @Setter
    public static class ResUpdateOnlineResumeDTO extends BaseResumeDTO {
        private Instant updatedAt;
    }

    @Getter
    @Setter
    public static class ResGetOnlineResumeDTO extends BaseResumeDTO {
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Getter
    @Setter
    private static class SkillInfo {
        private long id;
        private String name;
    }

    @Getter
    @Setter
    private static class UserInfo {
        private long id;
        private String email;
    }

    public static ResCreateOnlineResumeDTO convertToCreateDTO(OnlineResume resume) {
        ResCreateOnlineResumeDTO dto = new ResCreateOnlineResumeDTO();
        mapBaseFields(dto, resume);
        dto.setCreatedAt(resume.getCreatedAt());
        return dto;
    }

    public static ResUpdateOnlineResumeDTO convertToUpdateDTO(OnlineResume resume) {
        ResUpdateOnlineResumeDTO dto = new ResUpdateOnlineResumeDTO();
        mapBaseFields(dto, resume);
        dto.setUpdatedAt(resume.getUpdatedAt());
        return dto;
    }
    
    public static ResGetOnlineResumeDTO convertToGetDTO(OnlineResume resume) {
        ResGetOnlineResumeDTO dto = new ResGetOnlineResumeDTO();
        mapBaseFields(dto, resume);
        dto.setCreatedAt(resume.getCreatedAt());
        dto.setUpdatedAt(resume.getUpdatedAt());
        return dto;
    }

    private static void mapBaseFields(BaseResumeDTO dto, OnlineResume resume) {
        dto.setId(resume.getId());
        dto.setTitle(resume.getTitle());
        dto.setFullName(resume.getFullName());
        dto.setEmail(resume.getEmail());
        dto.setPhone(resume.getPhone());
        dto.setAddress(resume.getAddress());
        dto.setSummary(resume.getSummary());
        dto.setCertifications(resume.getCertifications());
        dto.setEducations(resume.getEducations());
        dto.setLanguages(resume.getLanguages());

        if (resume.getSkills() != null) {
            dto.setSkills(resume.getSkills().stream().map(skill -> {
                SkillInfo info = new SkillInfo();
                info.setId(skill.getId());
                info.setName(skill.getName());
                return info;
            }).collect(Collectors.toList()));
        }

        if (resume.getUser() != null) {
            UserInfo userInfo = new UserInfo();
            userInfo.setId(resume.getUser().getId());
            userInfo.setEmail(resume.getUser().getEmail());
            dto.setUser(userInfo);
        }
    }
}
