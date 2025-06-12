package vn.hoidanit.jobhunter.domain.request;

import jakarta.validation.constraints.NotBlank;

public class ReqChangePasswordDTO {
    private String oldPassword;
    @NotBlank(message = "Mật khẩu mới không được để trống")
    private String newPassword;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}