package vn.hoidanit.jobhunter.domain.response;

import java.util.List;

public class ResBulkCreateUserDTO {
    private int total;
    private int success;
    private int failed;
    private List<String> failedEmails;

    public ResBulkCreateUserDTO() {
    }

    public ResBulkCreateUserDTO(int total, int success, int failed, List<String> failedEmails) {
        this.total = total;
        this.success = success;
        this.failed = failed;
        this.failedEmails = failedEmails;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getSuccess() {
        return success;
    }

    public void setSuccess(int success) {
        this.success = success;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public List<String> getFailedEmails() {
        return failedEmails;
    }

    public void setFailedEmails(List<String> failedEmails) {
        this.failedEmails = failedEmails;
    }
}