package vn.hoidanit.jobhunter.domain.request;

import jakarta.validation.constraints.NotBlank;

public class ReqSendOtpDTO {
    @NotBlank(message = "Email không được để trống")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}