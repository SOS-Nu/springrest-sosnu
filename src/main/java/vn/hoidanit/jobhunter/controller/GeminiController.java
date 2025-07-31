package vn.hoidanit.jobhunter.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.hoidanit.jobhunter.domain.response.ResFindCandidatesDTO;
import vn.hoidanit.jobhunter.domain.response.ResInitiateCandidateSearchDTO;
import vn.hoidanit.jobhunter.domain.response.ai.ResCvEvaluationDTO;
import vn.hoidanit.jobhunter.domain.response.job.ResFetchJobDTO;
import vn.hoidanit.jobhunter.domain.response.job.ResFindJobsDTO;
import vn.hoidanit.jobhunter.domain.response.job.ResInitiateSearchDTO;
import vn.hoidanit.jobhunter.domain.response.job.ResJobWithScoreDTO;
import vn.hoidanit.jobhunter.service.ExchangeRateService;
import vn.hoidanit.jobhunter.service.FileService;
import vn.hoidanit.jobhunter.service.GeminiService;
import vn.hoidanit.jobhunter.util.annotation.ApiMessage;
import vn.hoidanit.jobhunter.util.error.IdInvalidException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/gemini")
public class GeminiController {

    private final GeminiService geminiService;
    private final FileService fileService;
    private final ExchangeRateService exchangeRateService; // THÊM BIẾN NÀY

    public GeminiController(GeminiService geminiService, FileService fileService,
            ExchangeRateService exchangeRateService) {
        this.geminiService = geminiService;
        this.fileService = fileService;
        this.exchangeRateService = exchangeRateService;
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

        if (file != null && !file.isEmpty()) {
            finalJobDescription = fileService.extractTextFromFile(file);
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
    public Object initiateSearch( // Kiểu trả về là Object
            @RequestParam(name = "skillsDescription", required = false) String skillsDescription,
            @RequestParam(name = "file", required = false) MultipartFile file,
            Pageable pageable,
            @RequestHeader(value = "Accept-Language", required = false, defaultValue = "vi") String language)
            throws IdInvalidException, IOException {

        byte[] cvFileBytes = (file != null && !file.isEmpty()) ? file.getBytes() : null;

        if ((skillsDescription == null || skillsDescription.trim().isEmpty()) && cvFileBytes == null) {
            throw new IdInvalidException("Vui lòng cung cấp mô tả kỹ năng hoặc tải lên file CV.");
        }

        // 1. Gọi service để lấy dữ liệu gốc
        ResInitiateSearchDTO serviceResult = geminiService.initiateJobSearch(skillsDescription, cvFileBytes,
                pageable);

        // 2. Chuyển đổi salary và tạo danh sách jobs cuối cùng
        List<Map<String, Object>> convertedJobs = convertJobsResponse(serviceResult.getJobs(), language);

        // 3. Tạo payload cho trường "data"
        Map<String, Object> dataPayload = new HashMap<>();
        dataPayload.put("jobs", convertedJobs); // Key là "jobs"
        dataPayload.put("meta", serviceResult.getMeta());
        dataPayload.put("searchId", serviceResult.getSearchId());

        return dataPayload;
    }

    @GetMapping("/search-results")
    @ApiMessage("Bước 2: Lấy các trang kết quả tiếp theo bằng searchId")
    public Object getSearchResults( // Kiểu trả về là Object
            @RequestParam("searchId") String searchId,
            Pageable pageable,
            @RequestHeader(value = "Accept-Language", required = false, defaultValue = "vi") String language)
            throws IdInvalidException {

        // 1. Gọi service để lấy dữ liệu gốc
        ResFindJobsDTO serviceResult = geminiService.getJobSearchResults(searchId, pageable);

        // 2. Chuyển đổi salary và tạo danh sách jobs cuối cùng
        List<Map<String, Object>> convertedJobs = convertJobsResponse(serviceResult.getJobs(), language);

        // 3. Tạo payload cho trường "data"
        Map<String, Object> dataPayload = new HashMap<>();
        dataPayload.put("jobs", convertedJobs); // Key là "jobs"
        dataPayload.put("meta", serviceResult.getMeta());

        return dataPayload;
    }

    /**
     * Phương thức helper để biến đổi danh sách jobs từ service,
     * chủ yếu là để xử lý trường 'salary'.
     */
    private List<Map<String, Object>> convertJobsResponse(List<ResJobWithScoreDTO> serviceJobs, String language) {
        List<Map<String, Object>> finalList = new ArrayList<>();

        for (ResJobWithScoreDTO item : serviceJobs) {
            ResFetchJobDTO jobData = item.getJob();

            // Tạo object job mới để không thay đổi cấu trúc ResFetchJobDTO
            Map<String, Object> jobMap = new HashMap<>();
            jobMap.put("id", jobData.getId());
            jobMap.put("name", jobData.getName());
            jobMap.put("location", jobData.getLocation());
            jobMap.put("quantity", jobData.getQuantity());
            jobMap.put("level", jobData.getLevel());
            jobMap.put("description", jobData.getDescription());
            jobMap.put("startDate", jobData.getStartDate());
            jobMap.put("endDate", jobData.getEndDate());
            jobMap.put("active", jobData.isActive());
            jobMap.put("createdAt", jobData.getCreatedAt());
            jobMap.put("updatedAt", jobData.getUpdatedAt());
            jobMap.put("createdBy", jobData.getCreatedBy());
            jobMap.put("updatedBy", jobData.getUpdatedBy());
            jobMap.put("company", jobData.getCompany());
            jobMap.put("skills", jobData.getSkills());

            // Tạo object salary mới
            Map<String, Object> salaryInfo = new HashMap<>();
            if (language != null && language.startsWith("en")) {
                salaryInfo.put("value", exchangeRateService.convert(jobData.getSalary(), "VND", "USD"));
                salaryInfo.put("currency", "USD");
            } else {
                salaryInfo.put("value", jobData.getSalary());
                salaryInfo.put("currency", "VND");
            }
            // Ghi đè trường salary bằng object mới
            jobMap.put("salary", salaryInfo);

            // Tạo object cuối cùng chứa cả score và job đã biến đổi
            Map<String, Object> finalItem = new HashMap<>();
            finalItem.put("score", item.getScore());
            finalItem.put("job", jobMap);

            finalList.add(finalItem);
        }

        return finalList;
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
