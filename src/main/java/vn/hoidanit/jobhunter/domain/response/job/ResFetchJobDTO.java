package vn.hoidanit.jobhunter.domain.response.job;

import java.time.Instant;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResFetchJobDTO {
    private long id;
    private String name;
    private String location;
    private double salary;
    private int quantity;
    private String level;
    private String description;
    private Instant startDate;
    private Instant endDate;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
    private CompanyInfo company;
    private List<SkillInfo> skills;

    @Getter
    @Setter
    public static class CompanyInfo {
        private long id;
        private String name;
    }

    @Getter
    @Setter
    public static class SkillInfo {
        private long id;
        private String name;
    }
}