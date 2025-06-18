package vn.hoidanit.jobhunter.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class ReqCreateCompanyDTO {

    @NotBlank(message = "Tên công ty không được để trống")
    private String name;

    private String description;

    private String address;

    private String logo;

    
    private String field; // Lĩnh vực

    private String website; // Website

    private String scale; // Quy mô

    private String country; // Quốc gia

    private Integer establishedYear; // Năm thành lập
 
}