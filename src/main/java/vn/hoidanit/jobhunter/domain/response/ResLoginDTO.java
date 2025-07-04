package vn.hoidanit.jobhunter.domain.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.hoidanit.jobhunter.domain.entity.Role;
import vn.hoidanit.jobhunter.util.constant.GenderEnum;

@Getter
@Setter
public class ResLoginDTO {
    @JsonProperty("access_token")
    private String accessToken;

    private UserLogin user;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserLogin {
        private long id;
        private String email;
        private String name;
        private GenderEnum gender;
        private String address;
        private int age;
        private String avatar;
        private boolean isPublic;
        private Role role;
        private boolean isVip;
        private CompanyUser company;

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
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserGetAccount {
        private UserLogin user;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserInsideToken {
        private long id;
        private String email;
        private String name;
    }
}