package vn.hoidanit.jobhunter.domain.response;

import java.util.List;

public class ResBulkCreateJobDTO {
    private int total;
    private int success;
    private int failed;
    private List<String> failedJobs;

    public ResBulkCreateJobDTO() {
    }

    public ResBulkCreateJobDTO(int total, int success, int failed, List<String> failedJobs) {
        this.total = total;
        this.success = success;
        this.failed = failed;
        this.failedJobs = failedJobs;
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

    public List<String> getFailedJobs() {
        return failedJobs;
    }

    public void setFailedJobs(List<String> failedJobs) {
        this.failedJobs = failedJobs;
    }
}