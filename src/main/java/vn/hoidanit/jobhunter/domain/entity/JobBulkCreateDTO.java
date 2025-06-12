package vn.hoidanit.jobhunter.domain.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import vn.hoidanit.jobhunter.util.constant.LevelEnum;

import java.util.List;

public class JobBulkCreateDTO {

    @NotBlank(message = "Tên công việc không được để trống")
    private String name;

    @NotBlank(message = "Địa điểm không được để trống")
    private String location;

    @NotBlank(message = "Mức lương không được để trống")
    private String salary;

    @NotNull(message = "Công ty không được để trống")
    private CompanyDTO company;

    private int quantity;

    @NotNull(message = "Cấp độ không được để trống")
    private LevelEnum level;

    private String description;

    @NotBlank(message = "Ngày bắt đầu không được để trống")
    private String startDate;

    @NotBlank(message = "Ngày kết thúc không được để trống")
    private String endDate;

    private boolean active;

    @NotNull(message = "Kỹ năng không được để trống")
    private List<SkillDTO> skills;

    // Inner DTO for Company
    public static class CompanyDTO {
        @NotNull(message = "Company ID không được để trống")
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    // Inner DTO for Skill
    public static class SkillDTO {
        @NotNull(message = "Skill ID không được để trống")
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSalary() {
        return salary;
    }

    public void setSalary(String salary) {
        this.salary = salary;
    }

    public CompanyDTO getCompany() {
        return company;
    }

    public void setCompany(CompanyDTO company) {
        this.company = company;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public LevelEnum getLevel() {
        return level;
    }

    public void setLevel(LevelEnum level) {
        this.level = level;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<SkillDTO> getSkills() {
        return skills;
    }

    public void setSkills(List<SkillDTO> skills) {
        this.skills = skills;
    }
}