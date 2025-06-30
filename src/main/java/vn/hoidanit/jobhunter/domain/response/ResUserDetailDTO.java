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
    private String email; // Sẽ được gán giá trị có điều kiện
    private int age;
    private GenderEnum gender;
    private String address; // Sẽ được gán giá trị có điều kiện
    private String mainResume;
    private boolean isPublic; // Thêm trường này vào DTO
    private String avatar;

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
        dto.setPublic(user.isPublic()); // Gán giá trị isPublic

        // LOGIC CÓ ĐIỀU KIỆN: Kiểm tra isPublic trước khi gán email và address
        if (user.isPublic()) {
            dto.setEmail(user.getEmail());
            dto.setAddress(user.getAddress());
        } else {
            // Nếu không public, có thể gán null hoặc một giá trị ẩn
            dto.setEmail(null);
            dto.setAddress(null);
        }

        // Map thông tin OnlineResume nếu có
        if (user.getOnlineResume() != null) {
            dto.setOnlineResume(ResOnlineResumeDTO.convertToGetDTO(user.getOnlineResume()));
        }

        // Map danh sách WorkExperience nếu có
        if (user.getWorkExperiences() != null && !user.getWorkExperiences().isEmpty()) {
            dto.setWorkExperiences(
                    ResWorkExperienceDTO.convertToListResponse(user.getWorkExperiences()));
        }

        return dto;
    }
}