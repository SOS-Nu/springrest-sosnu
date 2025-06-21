package vn.hoidanit.jobhunter.domain.response;

import lombok.Getter;
import lombok.Setter;
import vn.hoidanit.jobhunter.domain.entity.User;
import vn.hoidanit.jobhunter.util.constant.GenderEnum;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class ResUserDetailDTO {

    private long id;
    private String name;
    private String email;
    private int age;
    private GenderEnum gender;
    private String address;
    private String mainResume;
    private boolean isPublic;

    private ResOnlineResumeDTO.ResGetOnlineResumeDTO onlineResume;
    private List<ResWorkExperienceDTO.WorkExperienceResponse> workExperiences;

    public static ResUserDetailDTO convertToDTO(User user) {
        ResUserDetailDTO dto = new ResUserDetailDTO();
        
        // Map thông tin cơ bản của User
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setAge(user.getAge());
        dto.setGender(user.getGender());
        dto.setMainResume(user.getMainResume());
        dto.setPublic(user.isPublic());

        // Chỉ hiển thị email và address nếu isPublic là true
        if (user.isPublic()) {
            dto.setEmail(user.getEmail());
            dto.setAddress(user.getAddress());
        }

        // Map thông tin OnlineResume nếu có
        if (user.getOnlineResume() != null) {
            dto.setOnlineResume(ResOnlineResumeDTO.convertToGetDTO(user.getOnlineResume()));
        }

        // Map danh sách WorkExperience nếu có
        if (user.getWorkExperiences() != null && !user.getWorkExperiences().isEmpty()) {
            dto.setWorkExperiences(
                ResWorkExperienceDTO.convertToListResponse(user.getWorkExperiences())
            );
        }

        return dto;
    }
}