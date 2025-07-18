package vn.hoidanit.jobhunter.domain.response;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
public class ResFetchCompanyDTO {
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
    private Instant createdAt;
    private Instant updatedAt;

    // private String createdBy;
    //
    // private String updatedBy;

    // New field to hold the count of active jobs
    private long totalJobs;

    private HrCompany hrCompany;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HrCompany {
        private long id;
        private String name;
        private String email;
    }

}