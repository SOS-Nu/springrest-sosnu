package vn.hoidanit.jobhunter.kafka;

import java.util.List;
import vn.hoidanit.jobhunter.domain.response.email.ResEmailJob;

public class MailCommand {
    private String to;
    private String subject;
    private String templateName; // "job"
    private String username;
    private List<Long> jobIds; // <-- CHỈ GỬI ID

    public MailCommand() {
    } // no-args cho Jackson

    public MailCommand(String to, String subject, String templateName, String username, List<Long> jobIds) {
        this.to = to;
        this.subject = subject;
        this.templateName = templateName;
        this.username = username;
        this.jobIds = jobIds;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<Long> getJobIds() {
        return jobIds;
    }

    public void setJobIds(List<Long> jobIds) {
        this.jobIds = jobIds;
    }
}
