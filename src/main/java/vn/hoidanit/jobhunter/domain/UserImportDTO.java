package vn.hoidanit.jobhunter.domain;

import lombok.Data;

@Data
public class UserImportDTO {
    private String name;
    private String email;
    private String password;
    private Integer age;
    private String gender; // MALE, FEMALE
    private String address;
    private Long roleId; // ID của Role
}
