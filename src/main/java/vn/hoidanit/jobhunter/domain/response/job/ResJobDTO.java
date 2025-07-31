package vn.hoidanit.jobhunter.domain.response.job;

import lombok.Getter;
import lombok.Setter;
import vn.hoidanit.jobhunter.domain.response.job.ResFetchJobDTO.SkillInfo;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class ResJobDTO {
    private long id;
    private String name;
    private String location;
    private String address;

    private SalaryInfo salary; // Sử dụng object lồng nhau
    private int quantity;
    private String level;
    private Instant startDate;
    private Instant endDate;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
    private List<SkillInfo> skills;
    private CompanyInfo company;

    @Getter
    @Setter
    public static class SalaryInfo {
        private double value;
        private String currency;
    }

    @Getter
    @Setter
    public static class CompanyInfo {
        private long id;
        private String name;
        private String logo;

    }

    @Getter
    @Setter
    public static class SkillInfo {
        private long id;
        private String name;
    }
}