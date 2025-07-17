package vn.hoidanit.jobhunter.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.hoidanit.jobhunter.domain.response.ResFindCandidatesDTO;
import vn.hoidanit.jobhunter.domain.response.job.ResFindJobsDTO;
import vn.hoidanit.jobhunter.service.FileService;
import vn.hoidanit.jobhunter.service.GeminiService;
import vn.hoidanit.jobhunter.util.annotation.ApiMessage;
import vn.hoidanit.jobhunter.util.error.IdInvalidException;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/gemini")
public class GeminiController {

    private final GeminiService geminiService;
    private final FileService fileService;

    public GeminiController(GeminiService geminiService, FileService fileService) {
        this.geminiService = geminiService;
        this.fileService = fileService;
    }

    @PostMapping("/find-candidates")
    @ApiMessage("Tìm ứng viên phù hợp bằng Gemini")
    public ResponseEntity<ResFindCandidatesDTO> findCandidates(
            @RequestParam(name = "jobDescription", required = false) String jobDescription,
            @RequestParam(name = "file", required = false) MultipartFile file) throws IdInvalidException, IOException {

        String finalJobDescription = jobDescription;

        if (file != null && !file.isEmpty()) {
            finalJobDescription = fileService.extractTextFromFile(file);
        }

        if (finalJobDescription == null || finalJobDescription.trim().isEmpty()) {
            throw new IdInvalidException("Vui lòng cung cấp mô tả công việc bằng văn bản hoặc file.");
        }

        ResFindCandidatesDTO result = geminiService.findCandidates(finalJobDescription);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/find-jobs")
    @ApiMessage("Tìm công việc phù hợp dựa trên CV hoặc kỹ năng")
    public ResponseEntity<ResFindJobsDTO> findJobs(
            @RequestParam(name = "skillsDescription", required = false) String skillsDescription,
            @RequestParam(name = "file", required = false) MultipartFile file,
            Pageable pageable) throws IdInvalidException, IOException { // <= THÊM PAGEABLE

        byte[] cvFileBytes = (file != null && !file.isEmpty()) ? file.getBytes() : null;
        String cvFileName = (file != null && !file.isEmpty()) ? file.getOriginalFilename() : null;

        if ((skillsDescription == null || skillsDescription.trim().isEmpty()) && cvFileBytes == null) {
            throw new IdInvalidException("Vui lòng cung cấp mô tả kỹ năng hoặc tải lên file CV.");
        }

        // Truyền pageable vào service
        ResFindJobsDTO result = geminiService.findJobsForCandidate(skillsDescription, cvFileBytes, cvFileName,
                pageable);
        return ResponseEntity.ok(result);
    }

}
