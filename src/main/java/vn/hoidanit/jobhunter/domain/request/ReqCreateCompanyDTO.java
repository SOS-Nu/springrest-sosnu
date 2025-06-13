package vn.hoidanit.jobhunter.domain.request;

import jakarta.validation.constraints.NotBlank;

public class ReqCreateCompanyDTO {

    @NotBlank(message = "Tên công ty không được để trống")
    private String name;

    private String description;

    private String address;

    private String logo;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }
}