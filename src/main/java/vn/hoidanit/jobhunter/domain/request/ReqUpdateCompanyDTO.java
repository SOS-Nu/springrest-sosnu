package vn.hoidanit.jobhunter.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqUpdateCompanyDTO {

    @NotBlank(message = "Tên công ty không được để trống")
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