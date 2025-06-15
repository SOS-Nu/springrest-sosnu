package vn.hoidanit.jobhunter.domain.entity;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import vn.hoidanit.jobhunter.util.SecurityUtil;

@Getter
@Setter
@Entity
@Table(name = "online_resumes")
public class OnlineResume extends BaseEntity {

    @NotBlank(message = "{content.not.blank}")
    private String title;

    @NotBlank(message = "{name.not.blank}")
    private String fullName;

    @NotBlank(message = "{email.not.blank}")
    @Email
    private String email;

    private String phone;

    private Date dateOfBirth;

    private String address;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String summary;

    @ManyToMany(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "onlineResumes" })
    @JoinTable(name = "online_resumes_skills", joinColumns = @JoinColumn(name = "online_resume_id"), inverseJoinColumns = @JoinColumn(name = "skill_id"))
    private List<Skill> skills;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String certifications;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String educations;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String languages;

    @OneToOne(mappedBy = "onlineResume")
    private User user;

    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;

    @PrePersist
    public void handleBeforeCreate() {
        this.createdBy = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get() : "";
        this.createdAt = Instant.now();
    }

    @PreUpdate
    public void handleBeforeUpdate() {
        this.updatedBy = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get() : "";
        this.updatedAt = Instant.now();
    }
}