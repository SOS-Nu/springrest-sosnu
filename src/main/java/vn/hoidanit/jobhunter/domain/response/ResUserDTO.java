package vn.hoidanit.jobhunter.domain.response;

import java.time.Instant;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.hoidanit.jobhunter.domain.entity.Company;
import vn.hoidanit.jobhunter.domain.response.ResLoginDTO.UserLogin.CompanyUser;
import vn.hoidanit.jobhunter.domain.response.chat.ResLastMessageDTO;
import vn.hoidanit.jobhunter.util.constant.GenderEnum;
import vn.hoidanit.jobhunter.util.constant.UserStatusEnum;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResUserDTO {
    private long id;
    private String email;
    private String name;
    private GenderEnum gender;
    private String address;
    private int age;
    private String avatar;

    private Instant updatedAt;
    private Instant createdAt;

    private CompanyUser company;

    @Enumerated(EnumType.STRING)
    private UserStatusEnum status;

    private RoleUser role;
    private ResLastMessageDTO lastMessage;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CompanyUser {
        private long id;
        private String name;
        private String description;
        private String address;

        private String logo;

        private String field;
        private String website;
        private String scale;
        private String country;
        private int foundingYear;
        private String location;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RoleUser {
        private long id;
        private String name;
    }

}
