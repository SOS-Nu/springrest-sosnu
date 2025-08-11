package vn.hoidanit.jobhunter.domain.dto;

import java.io.Serializable;
import java.time.Instant;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Data
public class PermissionDTO implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String name;

    private String apiPath;

    private String method;

    private String module;

    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;

}