package vn.hoidanit.jobhunter.domain.response;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResCreateCompanyDTO {

    private long id;
    private String name;
    private String description;
    private String address;
    private String logo;
    private Instant createdAt;

    // THÊM MỚI
    private String field;
    private String website;
    private String scale;
    private String country;
    private int foundingYear;
    private String location;

  
}