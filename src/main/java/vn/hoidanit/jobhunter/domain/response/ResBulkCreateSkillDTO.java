package vn.hoidanit.jobhunter.domain.response;

import java.util.List;

public class ResBulkCreateSkillDTO {
    private int total;
    private int success;
    private int failed;
    private List<String> failedSkills;

    public ResBulkCreateSkillDTO() {
    }

    public ResBulkCreateSkillDTO(int total, int success, int failed, List<String> failedSkills) {
        this.total = total;
        this.success = success;
        this.failed = failed;
        this.failedSkills = failedSkills;
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

    public List<String> getFailedSkills() {
        return failedSkills;
    }

    public void setFailedSkills(List<String> failedSkills) {
        this.failedSkills = failedSkills;
    }
}