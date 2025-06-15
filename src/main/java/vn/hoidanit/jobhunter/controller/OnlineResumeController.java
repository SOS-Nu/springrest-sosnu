package vn.hoidanit.jobhunter.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.hoidanit.jobhunter.domain.entity.OnlineResume;
import vn.hoidanit.jobhunter.domain.response.ResOnlineResumeDTO;
import vn.hoidanit.jobhunter.service.OnlineResumeService;
import vn.hoidanit.jobhunter.util.annotation.ApiMessage;
import vn.hoidanit.jobhunter.util.error.IdInvalidException;

@RestController
@RequestMapping("/api/v1/online-resumes")
public class OnlineResumeController {

    private final OnlineResumeService onlineResumeService;

    public OnlineResumeController(OnlineResumeService onlineResumeService) {
        this.onlineResumeService = onlineResumeService;
    }

    @PostMapping
    @ApiMessage("Tạo mới Online Resume")
    public ResponseEntity<ResOnlineResumeDTO.ResCreateOnlineResumeDTO> createOnlineResume(
            @Valid @RequestBody OnlineResume resume) throws IdInvalidException {
        OnlineResume newResume = this.onlineResumeService.createOnlineResume(resume);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResOnlineResumeDTO.convertToCreateDTO(newResume));
    }

    @PutMapping
    @ApiMessage("Cập nhật Online Resume")
    public ResponseEntity<ResOnlineResumeDTO.ResUpdateOnlineResumeDTO> updateOnlineResume(
            @Valid @RequestBody OnlineResume resume) throws IdInvalidException {
        OnlineResume updatedResume = this.onlineResumeService.updateOnlineResume(resume);
        return ResponseEntity.ok(ResOnlineResumeDTO.convertToUpdateDTO(updatedResume));
    }

    @DeleteMapping()
    @ApiMessage("Xóa Online Resume")
    public ResponseEntity<Void> deleteOnlineResume() throws IdInvalidException {
        this.onlineResumeService.deleteOnlineResume();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/my-resume")
    @ApiMessage("Lấy Online Resume của tôi")
    public ResponseEntity<ResOnlineResumeDTO.ResGetOnlineResumeDTO> getMyOnlineResume() throws IdInvalidException {
        return this.onlineResumeService.getMyOnlineResume()
                .map(resume -> ResponseEntity.ok(ResOnlineResumeDTO.convertToGetDTO(resume)))
                .orElse(ResponseEntity.notFound().build());
    }
}
