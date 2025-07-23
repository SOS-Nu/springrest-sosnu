package vn.hoidanit.jobhunter.domain.request;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import vn.hoidanit.jobhunter.util.constant.LevelEnum;

@Getter
@Setter
public class ReqUpdateJobDTO {

    @NotNull(message = "Id công việc không được để trống")
    private long id;

    @NotBlank(message = "Tên công việc không được để trống")
    private String name;

    @NotBlank(message = "Địa điểm không được để trống")
    private String location;

    private String address;

    @NotNull(message = "Lương không được để trống")
    private Double salary;

    @NotNull(message = "Số lượng không được để trống")
    private Integer quantity;

    @NotNull(message = "Cấp độ không được để trống")
    private LevelEnum level;

    private String description;

    private Instant startDate;
    private Instant endDate;
    private boolean active;

    // ========== CHANGE START ==========
    private List<SkillDTO> skills;

    @Getter
    @Setter
    public static class SkillDTO {
        @NotNull
        private long id;
    }
    // ========== CHANGE END ==========
}