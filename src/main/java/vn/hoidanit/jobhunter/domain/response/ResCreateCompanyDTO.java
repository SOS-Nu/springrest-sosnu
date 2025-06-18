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

    private String field; // Lĩnh vực
    private String website; // Website
    private String scale; // Quy mô
    private String country; // Quốc gia
    private Integer establishedYear; // Năm thành lập

  
}