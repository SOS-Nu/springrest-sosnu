package vn.hoidanit.jobhunter.domain.request;

import jakarta.validation.constraints.NotBlank;

public class ReqGoogleLoginDTO {
    @NotBlank(message = "Credential không được để trống")
    private String credential;

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }
}