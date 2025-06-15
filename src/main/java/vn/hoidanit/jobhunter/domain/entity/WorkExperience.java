package vn.hoidanit.jobhunter.domain.entity;

import java.time.Instant;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "work_experiences")
public class WorkExperience extends BaseEntity {

    @NotBlank(message = "Tên công ty không được để trống")
    private String companyName;

    private Date startDate;

    private Date endDate;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String description;

    private String location;

    // Quan hệ nhiều-một: Nhiều WorkExperience thuộc về một User
    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore // Quan trọng: Tránh vòng lặp vô hạn khi serialize JSON
    private User user;

    // Các trường createdBy, updatedBy, etc. đã có trong BaseEntity nên không cần khai báo lại
}
