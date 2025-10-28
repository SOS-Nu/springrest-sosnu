package vn.hoidanit.jobhunter.domain.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true) // bơ đi những thuộc tính truyền dư vào
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