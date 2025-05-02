package vn.hoidanit.jobhunter.domain;

import jakarta.validation.constraints.NotBlank;

public class SkillBulkCreateDTO {

    @NotBlank(message = "Tên kỹ năng không được để trống")
    private String name;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}