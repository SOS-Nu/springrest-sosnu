package vn.hoidanit.jobhunter.domain.entity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.BatchSize;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import vn.hoidanit.jobhunter.util.SecurityUtil;
import vn.hoidanit.jobhunter.util.constant.GenderEnum;
import vn.hoidanit.jobhunter.util.constant.UserStatusEnum;

@NamedEntityGraph(name = "graph.user.role", attributeNodes = { @NamedAttributeNode("role") })
@NamedEntityGraph(name = "graph.user.details", attributeNodes = {
                @NamedAttributeNode("company"),
                @NamedAttributeNode("role"),
                @NamedAttributeNode("onlineResume"),
                // CHỈ GIỮ LẠI MỘT LIST
                @NamedAttributeNode(value = "resumes", subgraph = "subgraph.resume.details")
// LOẠI BỎ workExperiences VÀ paymentHistories KHỎI GRAPH NÀY
}, subgraphs = {
                @NamedSubgraph(name = "subgraph.resume.details", attributeNodes = {
                                @NamedAttributeNode(value = "job", subgraph = "subgraph.job.company") }),
                @NamedSubgraph(name = "subgraph.job.company", attributeNodes = { @NamedAttributeNode("company") })
})
@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private long id;

        private String name;

        @NotBlank(message = "email không được để trống")
        private String email;

        private String password;

        private int age;
        private String mainResume;

        @Enumerated(EnumType.STRING)
        private GenderEnum gender;

        private String address;

        // @Column(columnDefinition = "MEDIUMTEXT")
        // private String refreshToken;

        @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
        @JsonIgnore
        private List<UserSession> sessions; // <<< THÊM QUAN HỆ NÀY

        private Instant createdAt;
        private Instant updatedAt;
        private String createdBy;
        private String updatedBy;
        private String avatar;
        private boolean isVip;
        private LocalDateTime vipExpiryDate;
        private int cvSubmissionCount;

        // TÍCH HỢP TRƯỜNG MỚI
        @Column(columnDefinition = "boolean default true")
        private boolean isPublic = true;

        @BatchSize(size = 25)
        @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
        @JsonIgnore
        private List<PaymentHistory> paymentHistories;

        @ManyToOne
        @JoinColumn(name = "company_id")
        private Company company;

        @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
        @JsonIgnore
        List<Resume> resumes;

        @ManyToOne
        @JoinColumn(name = "role_id")
        private Role role;

        @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
        @JoinColumn(name = "online_resume_id", referencedColumnName = "id")
        private OnlineResume onlineResume;

        @BatchSize(size = 25)
        @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<WorkExperience> workExperiences;

        @Column(name = "last_security_update_at")
        private Instant lastSecurityUpdateAt;

        @PrePersist
        public void handleBeforeCreate() {
                this.createdBy = SecurityUtil.getCurrentUserLogin().isPresent() == true
                                ? SecurityUtil.getCurrentUserLogin().get()
                                : "";
                this.createdAt = Instant.now();
        }

        @Enumerated(EnumType.STRING)
        private UserStatusEnum status;

        @PreUpdate
        public void handleBeforeUpdate() {
                this.updatedBy = SecurityUtil.getCurrentUserLogin().isPresent() == true
                                ? SecurityUtil.getCurrentUserLogin().get()
                                : "";
                this.updatedAt = Instant.now();
        }
}