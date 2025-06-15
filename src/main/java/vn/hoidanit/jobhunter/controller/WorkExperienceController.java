package vn.hoidanit.jobhunter.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.hoidanit.jobhunter.domain.entity.WorkExperience;
import vn.hoidanit.jobhunter.domain.response.ResWorkExperienceDTO;
import vn.hoidanit.jobhunter.service.WorkExperienceService;
import vn.hoidanit.jobhunter.util.annotation.ApiMessage;
import vn.hoidanit.jobhunter.util.error.IdInvalidException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/work-experiences")
public class WorkExperienceController {

    private final WorkExperienceService workExperienceService;

    public WorkExperienceController(WorkExperienceService workExperienceService) {
        this.workExperienceService = workExperienceService;
    }

    @PostMapping
    @ApiMessage("Tạo mới kinh nghiệm làm việc")
    public ResponseEntity<ResWorkExperienceDTO.WorkExperienceResponse> createWorkExperience(
            @Valid @RequestBody WorkExperience workExperience) throws IdInvalidException {
        WorkExperience newExp = workExperienceService.createWorkExperience(workExperience);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResWorkExperienceDTO.convertToResponse(newExp));
    }

    @PutMapping
    @ApiMessage("Cập nhật kinh nghiệm làm việc")
    public ResponseEntity<ResWorkExperienceDTO.WorkExperienceResponse> updateWorkExperience(
            @Valid @RequestBody WorkExperience workExperience) throws IdInvalidException {
        WorkExperience updatedExp = workExperienceService.updateWorkExperience(workExperience);
        return ResponseEntity.ok(ResWorkExperienceDTO.convertToResponse(updatedExp));
    }

    @DeleteMapping("/{id}")
    @ApiMessage("Xóa kinh nghiệm làm việc")
    public ResponseEntity<Void> deleteWorkExperience(@PathVariable("id") long id) throws IdInvalidException {
        workExperienceService.deleteWorkExperience(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/my-experiences")
    @ApiMessage("Lấy danh sách kinh nghiệm làm việc của tôi")
    public ResponseEntity<List<ResWorkExperienceDTO.WorkExperienceResponse>> getMyWorkExperiences() throws IdInvalidException {
        List<WorkExperience> myExperiences = workExperienceService.getMyWorkExperiences();
        return ResponseEntity.ok(ResWorkExperienceDTO.convertToListResponse(myExperiences));
    }
}
