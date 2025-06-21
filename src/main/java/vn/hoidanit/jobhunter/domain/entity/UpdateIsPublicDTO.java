package vn.hoidanit.jobhunter.domain.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateIsPublicDTO {
    private long id;
    private boolean isPublic;
}