package vn.hoidanit.jobhunter.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.hoidanit.jobhunter.domain.response.ResFindCandidatesDTO;
import vn.hoidanit.jobhunter.domain.response.ResInitiateCandidateSearchDTO;
import vn.hoidanit.jobhunter.domain.response.ai.ResCvEvaluationDTO;
import vn.hoidanit.jobhunter.domain.response.job.ResFindJobsDTO;
import vn.hoidanit.jobhunter.domain.response.job.ResInitiateSearchDTO;
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

    // @PostMapping("/find-candidates")
    // @ApiMessage("Tìm ứng viên phù hợp bằng Gemini")
    // public ResponseEntity<ResFindCandidatesDTO> findCandidates(
    // @RequestParam(name = "jobDescription", required = false) String
    // jobDescription,
    // @RequestParam(name = "file", required = false) MultipartFile file) throws
    // IdInvalidException, IOException {
    //
    // String finalJobDescription = jobDescription;
    //
    // if (file != null && !file.isEmpty()) {
    // finalJobDescription = fileService.extractTextFromFile(file);
    // }
    //
    // if (finalJobDescription == null || finalJobDescription.trim().isEmpty()) {
    // throw new IdInvalidException("Vui lòng cung cấp mô tả công việc bằng văn bản
    // hoặc file.");
    // }
    //
    // ResFindCandidatesDTO result =
    // geminiService.findCandidates(finalJobDescription);
    // return ResponseEntity.ok(result);
    // }

    // ================= START: API TÌM KIẾM ỨNG VIÊN MỚI =================

    @PostMapping("/initiate-candidate-search")
    @ApiMessage("Bước 1: Khởi tạo tìm kiếm ứng viên, xử lý và nhận trang đầu tiên")
    public ResponseEntity<ResInitiateCandidateSearchDTO> initiateCandidateSearch(
            @RequestParam(name = "jobDescription", required = false) String jobDescription,
            @RequestParam(name = "file", required = false) MultipartFile file,
            Pageable pageable) throws IdInvalidException, IOException {

        String finalJobDescription = jobDescription;
        boolean isFileUploaded = file != null && !file.isEmpty();

        if (isFileUploaded) {
            // >>> THAY ĐỔI CỐT LÕI <<<
            // Đọc file thành byte array trước để đảm bảo an toàn,
            // sau đó mới trích xuất text từ byte array đó.
            byte[] fileBytes = file.getBytes();
            String extractedText = fileService.extractTextFromBytes(fileBytes); // Sử dụng phương thức an toàn hơn

            if (extractedText == null || extractedText.trim().isEmpty()) {
                throw new IdInvalidException(
                        "Không thể trích xuất nội dung văn bản từ file. File có thể trống hoặc bị lỗi.");
            }
            finalJobDescription = extractedText;
        }

        if (finalJobDescription == null || finalJobDescription.trim().isEmpty()) {
            throw new IdInvalidException("Vui lòng cung cấp mô tả công việc bằng văn bản hoặc file.");
        }

        ResInitiateCandidateSearchDTO result = geminiService.initiateCandidateSearch(finalJobDescription, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/candidate-search-results")
    @ApiMessage("Bước 2: Lấy các trang kết quả ứng viên tiếp theo bằng searchId")
    public ResponseEntity<ResFindCandidatesDTO> getCandidateSearchResults(
            @RequestParam("searchId") String searchId,
            Pageable pageable) throws IdInvalidException {

        ResFindCandidatesDTO result = geminiService.getCandidateSearchResults(searchId, pageable);
        return ResponseEntity.ok(result);
    }

    // ================= END: API TÌM KIẾM ỨNG VIÊN MỚI =================

    // @PostMapping("/find-jobs")
    // @ApiMessage("Tìm công việc phù hợp dựa trên CV hoặc kỹ năng")
    // public ResponseEntity<ResFindJobsDTO> findJobs(
    // @RequestParam(name = "skillsDescription", required = false) String
    // skillsDescription,
    // @RequestParam(name = "file", required = false) MultipartFile file,
    // Pageable pageable) throws IdInvalidException, IOException { // <= THÊM
    // PAGEABLE
    //
    // byte[] cvFileBytes = (file != null && !file.isEmpty()) ? file.getBytes() :
    // null;
    // String cvFileName = (file != null && !file.isEmpty()) ?
    // file.getOriginalFilename() : null;
    //
    // if ((skillsDescription == null || skillsDescription.trim().isEmpty()) &&
    // cvFileBytes == null) {
    // throw new IdInvalidException("Vui lòng cung cấp mô tả kỹ năng hoặc tải lên
    // file CV.");
    // }
    //
    // // Truyền pageable vào service
    // ResFindJobsDTO result = geminiService.findJobsForCandidate(skillsDescription,
    // cvFileBytes, cvFileName,
    // pageable);
    // return ResponseEntity.ok(result);
    // }

    @PostMapping("/initiate-search")
    @ApiMessage("Bước 1: Khởi tạo tìm kiếm việc làm và nhận trang đầu tiên")
    public ResponseEntity<ResInitiateSearchDTO> initiateSearch(
            @RequestParam(name = "skillsDescription", required = false) String skillsDescription,
            @RequestParam(name = "file", required = false) MultipartFile file,
            Pageable pageable) throws IdInvalidException, IOException {

        byte[] cvFileBytes = (file != null && !file.isEmpty()) ? file.getBytes() : null;
        String cvFileName = (file != null && !file.isEmpty()) ? file.getOriginalFilename() : null;

        if ((skillsDescription == null || skillsDescription.trim().isEmpty()) && cvFileBytes == null) {
            throw new IdInvalidException("Vui lòng cung cấp mô tả kỹ năng hoặc tải lên file CV.");
        }

        ResInitiateSearchDTO result = geminiService.initiateJobSearch(skillsDescription, cvFileBytes,
                pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/search-results")
    @ApiMessage("Bước 2: Lấy các trang kết quả tiếp theo bằng searchId")
    public ResponseEntity<ResFindJobsDTO> getSearchResults(
            @RequestParam("searchId") String searchId,
            Pageable pageable) throws IdInvalidException {

        ResFindJobsDTO result = geminiService.getJobSearchResults(searchId, pageable);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/evaluate-cv")
    public ResponseEntity<ResCvEvaluationDTO> evaluateCv(
            @RequestParam(value = "cvFile", required = false) MultipartFile cvFile,
            // <<< THÊM THAM SỐ NGÔN NGỮ >>>
            @RequestParam(value = "language", defaultValue = "vi") String language)
            throws IdInvalidException, IOException {

        // Truyền tham số ngôn ngữ vào service
        ResCvEvaluationDTO result = this.geminiService.evaluateCandidateCv(cvFile, language);
        return ResponseEntity.ok(result);
    }
}
