package vn.hoidanit.jobhunter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import vn.hoidanit.jobhunter.domain.entity.Job;
import vn.hoidanit.jobhunter.domain.response.ResCandidateWithScoreDTO;
import vn.hoidanit.jobhunter.domain.response.ResFindCandidatesDTO;
import vn.hoidanit.jobhunter.domain.response.ResUserDetailDTO;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.domain.response.job.ResFetchJobDTO;
import vn.hoidanit.jobhunter.domain.response.job.ResFindJobsDTO;
import vn.hoidanit.jobhunter.domain.response.job.ResJobWithScoreDTO;
import vn.hoidanit.jobhunter.repository.JobRepository;
import vn.hoidanit.jobhunter.util.SecurityUtil;
import vn.hoidanit.jobhunter.util.error.IdInvalidException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GeminiService {

    // ... (Giữ nguyên các biến và constructor)
    private final RestTemplate restTemplate;
    private final UserService userService;
    private final FileService fileService;
    private final ObjectMapper objectMapper;
    private final JobRepository jobRepository; // THÊM REPOSITORY NÀY
    private final JobService jobService; // THÊM SERVICE NÀY

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    public GeminiService(RestTemplate restTemplate, UserService userService, FileService fileService,
            ObjectMapper objectMapper, JobRepository jobRepository, JobService jobService) {
        this.restTemplate = restTemplate;
        this.userService = userService;
        this.fileService = fileService;
        this.objectMapper = objectMapper;
        this.jobRepository = jobRepository;
        this.jobService = jobService;
    }

    public ResFindCandidatesDTO findCandidates(String jobDescription) throws IdInvalidException {
        List<ResCandidateWithScoreDTO> suitableCandidates = new ArrayList<>();
        ResultPaginationDTO.Meta lastMeta = null;
        int currentPage = 0;
        final int PAGE_SIZE = 200;
        final int TARGET_CANDIDATES = 10;

        while (suitableCandidates.size() < TARGET_CANDIDATES) {
            Pageable pageable = PageRequest.of(currentPage, PAGE_SIZE);
            ResultPaginationDTO paginatedUsers = userService.fetchAllUserDetails(null, pageable);
            lastMeta = paginatedUsers.getMeta();

            @SuppressWarnings("unchecked")
            List<ResUserDetailDTO> currentUsers = (List<ResUserDetailDTO>) paginatedUsers.getResult();
            if (currentUsers.isEmpty()) {
                break; // Hết người dùng để kiểm tra
            }

            // Chuyển danh sách thành Map để tra cứu nhanh hơn
            Map<Long, ResUserDetailDTO> userMap = currentUsers.stream()
                    .collect(Collectors.toMap(ResUserDetailDTO::getId, Function.identity()));

            List<GeminiScoreResponse> rankedUsers = rankUsersWithGemini(jobDescription, currentUsers);

            for (GeminiScoreResponse rankedUser : rankedUsers) {
                if (suitableCandidates.size() >= TARGET_CANDIDATES)
                    break;

                ResUserDetailDTO userDetail = userMap.get(rankedUser.getUserId());
                if (userDetail != null) {
                    suitableCandidates.add(new ResCandidateWithScoreDTO(rankedUser.getScore(), userDetail));
                }
            }

            currentPage++;
        }

        // Sắp xếp danh sách cuối cùng theo điểm số giảm dần
        suitableCandidates.sort(Comparator.comparingInt(ResCandidateWithScoreDTO::getScore).reversed());

        return new ResFindCandidatesDTO(suitableCandidates, lastMeta);
    }

    /**
     * ĐÃ CẬP NHẬT: Gửi nội dung text của CV thay vì file base64
     */
    private List<GeminiScoreResponse> rankUsersWithGemini(String jobDescription, List<ResUserDetailDTO> users) {
        List<Map<String, Object>> parts = new ArrayList<>();

        String initialPrompt = "You are an expert HR assistant. Based on the following job description, please analyze each candidate. "
                + "Each candidate's information is provided as a combination of structured JSON data and their resume text (if available). "
                + "Prioritize the information in the resume text. "
                + "\n\nJob Description:\n\"" + jobDescription + "\"\n\n";
        parts.add(Map.of("text", initialPrompt));

        for (ResUserDetailDTO user : users) {
            try {
                String resumeFileName = user.getMainResume();
                user.setMainResume(null); // Tạm thời set null để không đưa vào JSON
                String userJson = objectMapper.writeValueAsString(user);
                user.setMainResume(resumeFileName); // Gán lại

                parts.add(Map.of("text", "Candidate data: " + userJson));

                if (resumeFileName != null && !resumeFileName.isEmpty()) {
                    // Trích xuất text từ file thay vì đọc bytes và encode
                    String resumeText = fileService.extractTextFromStoredFile(resumeFileName, "resumes");
                    if (resumeText != null && !resumeText.trim().isEmpty()) {
                        parts.add(Map.of("text", "Candidate Resume Text:\n" + resumeText));
                    } else {
                        parts.add(Map.of("text", "Candidate has a resume file, but no text could be extracted."));
                    }
                } else {
                    parts.add(Map.of("text", "Candidate has no resume file."));
                }

            } catch (IOException | URISyntaxException e) {
                System.err.println(
                        "Error processing user data or file for user ID " + user.getId() + ": " + e.getMessage());
            }
        }

        String finalPrompt = "\n\nAfter analyzing all candidates, return a JSON array of objects. Each object must have two keys: 'userId' (a number) and 'score' (a number from 0 to 100 representing how well they match the job description). "
                + "Only include candidates who are a good match. Rank the array from the highest score to the lowest. "
                + "Response (JSON array of objects only):";
        parts.add(Map.of("text", finalPrompt));

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> content = new HashMap<>();
            content.put("parts", parts);
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", Collections.singletonList(content));
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            String url = geminiApiUrl + "?key=" + geminiApiKey;
            String response = restTemplate.postForObject(url, entity, String.class);
            JsonNode root = objectMapper.readTree(response);
            String textResponse = root.path("candidates").path(0).path("content").path("parts").path(0).path("text")
                    .asText();
            String cleanedJson = textResponse.replace("```json", "").replace("```", "").trim();

            return objectMapper.readValue(cleanedJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, GeminiScoreResponse.class));

        } catch (Exception e) {
            System.err.println("Error calling Gemini API or parsing response: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    // Helper class để parse response từ Gemini
    @Getter
    @Setter
    private static class GeminiScoreResponse {
        private long userId;
        private int score;
    }

    private String getMimeType(String fileName) {
        if (fileName == null)
            return "application/octet-stream";
        String lowerCaseFileName = fileName.toLowerCase();
        if (lowerCaseFileName.endsWith(".pdf"))
            return "application/pdf";
        if (lowerCaseFileName.endsWith(".docx"))
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        // ... các mime type khác
        return "application/octet-stream";
    }

    /**
     * PHƯƠNG THỨC MỚI: Chấm điểm một CV so với mô tả công việc.
     * 
     * @param job         Thông tin chi tiết về công việc.
     * @param cvFileBytes Nội dung file CV dưới dạng byte array.
     * @param cvFileName  Tên file CV để xác định mime-type.
     * @return Điểm số từ 0-100.
     */
    /**
     * ĐÃ CẬP NHẬT: Chấm điểm CV dựa vào nội dung text.
     */
    /**
     * PHƯƠNG THỨC NÀY LÀ CỐT LÕI CỦA YÊU CẦU
     * Nó nhận bytes, chuyển sang text và gửi cho Gemini.
     */
    public int scoreCvAgainstJob(Job job, byte[] cvFileBytes, String cvFileName) {
        String jobDetails = String.format(
                "Job Title: %s\nLocation: %s\nLevel: %s\nDescription: %s",
                job.getName(), job.getLocation(), job.getLevel(), job.getDescription());

        List<Map<String, Object>> parts = new ArrayList<>();

        // Sử dụng FileService để chuyển byte[] thành text
        String cvText;
        try {
            cvText = fileService.extractTextFromBytes(cvFileBytes);
        } catch (IOException e) {
            System.err.println("Lỗi khi trích xuất text từ CV bytes: " + e.getMessage());
            return 0; // Trả về 0 nếu không trích xuất được
        }

        if (cvText == null || cvText.trim().isEmpty()) {
            System.err.println("Nội dung text của CV rỗng, bỏ qua chấm điểm.");
            return 0;
        }

        // Build prompt với nội dung text
        String promptText = "As an expert HR, please evaluate the following resume text against the job description. " +
                "Provide a suitability score on a scale of 0 to 100. " +
                "Return ONLY a JSON object with a single key 'score'. Example: {\"score\": 95}\n\n" +
                "Job Description:\n" + jobDetails + "\n\n" +
                "Candidate's Resume Text:\n" + cvText;
        parts.add(Map.of("text", promptText));

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> content = Map.of("parts", parts);
            Map<String, Object> requestBody = Map.of("contents", Collections.singletonList(content));
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String url = geminiApiUrl + "?key=" + geminiApiKey;
            String response = restTemplate.postForObject(url, entity, String.class);

            JsonNode root = objectMapper.readTree(response);
            String textResponse = root.path("candidates").path(0).path("content").path("parts").path(0).path("text")
                    .asText();
            String cleanedJson = textResponse.replace("```json", "").replace("```", "").trim();
            JsonNode scoreNode = objectMapper.readTree(cleanedJson);

            return scoreNode.path("score").asInt(0);

        } catch (Exception e) {
            System.err.println("Lỗi khi gọi Gemini API để chấm điểm: " + e.getMessage());
            return 0;
        }
    }

    public ResFindJobsDTO findJobsForCandidate(
            String skillsDescription, byte[] cvFileBytes, String cvFileName, Pageable pageable)
            throws IdInvalidException, IOException {

        final int PRE_FILTER_LIMIT = 200; // Lọc sơ bộ lấy 150 jobs tiềm năng nhất

        // Tạo cache key từ nội dung yêu cầu
        String cvTextForCache = (cvFileBytes != null) ? fileService.extractTextFromBytes(cvFileBytes) : "";
        String cacheKey = "find_jobs:" + SecurityUtil.hash(cvTextForCache + skillsDescription);

        // Cache cache = cacheManager.getCache("jobMatches");
        // List<ResJobWithScoreDTO> allRankedJobs = cache.get(cacheKey, List.class);
        List<ResJobWithScoreDTO> allRankedJobs = null; // Giả sử chưa có cache

        if (allRankedJobs == null) {
            // === GIAI ĐOẠN 1: LỌC SƠ BỘ CÁC JOB ĐANG ACTIVE ===
            Set<String> keywords = extractKeywords(cvTextForCache, skillsDescription);
            List<Job> potentialJobs = preFilterActiveJobs(keywords, PRE_FILTER_LIMIT);

            if (potentialJobs.isEmpty()) {
                return new ResFindJobsDTO(Collections.emptyList(), new ResultPaginationDTO.Meta());
            }

            // === GIAI ĐOẠN 2: CHẤM ĐIỂM BẰNG AI ===
            List<GeminiJobScoreResponse> rankedJobScores = rankJobsWithGemini(
                    skillsDescription, cvFileBytes, cvFileName, potentialJobs);

            Map<Long, Job> jobMap = potentialJobs.stream()
                    .collect(Collectors.toMap(Job::getId, Function.identity()));

            allRankedJobs = new ArrayList<>();
            for (GeminiJobScoreResponse rankedJob : rankedJobScores) {
                Job jobDetail = jobMap.get(rankedJob.getJobId());
                if (jobDetail != null) {
                    ResFetchJobDTO jobDTO = jobService.convertToResFetchJobDTO(jobDetail);
                    allRankedJobs.add(new ResJobWithScoreDTO(rankedJob.getScore(), jobDTO));
                }
            }

            allRankedJobs.sort(Comparator.comparingInt(ResJobWithScoreDTO::getScore).reversed());

            // Lưu vào cache
            // cache.put(cacheKey, allRankedJobs);
            System.out.println("LOG: Đã gọi AI và có thể lưu kết quả vào cache với key: " + cacheKey);
        } else {
            System.out.println("LOG: Đã tìm thấy kết quả trong cache với key: " + cacheKey);
        }

        // === PHÂN TRANG KẾT QUẢ TRẢ VỀ ===
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allRankedJobs.size());

        List<ResJobWithScoreDTO> paginatedResult = (start >= allRankedJobs.size())
                ? Collections.emptyList()
                : allRankedJobs.subList(start, end);

        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages((int) Math.ceil((double) allRankedJobs.size() / pageable.getPageSize()));
        meta.setTotal(allRankedJobs.size());

        return new ResFindJobsDTO(paginatedResult, meta);
    }

    /**
     * Helper method để lọc sơ bộ các job đang active bằng FTS và tìm theo skill.
     */
    private List<Job> preFilterActiveJobs(Set<String> keywords, int limit) {
        if (keywords.isEmpty()) {
            // Nếu không có keyword, lấy các jobs active mới nhất
            return jobRepository.findAll(
                    (root, query, cb) -> cb.isTrue(root.get("active")),
                    PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
        }

        String keywordString = String.join(" ", keywords);

        // 1. Lấy kết quả từ Full-Text Search (đã có điều kiện active=true)
        List<Job> ftsResults = jobRepository.searchActiveByKeywordsNative(keywordString, limit);

        // 2. Lấy kết quả từ tìm kiếm Skill (đã có điều kiện active=true)
        List<Job> skillResults = jobRepository.findActiveBySkillNames(keywords);

        // 3. Gộp kết quả và loại bỏ trùng lặp
        // Dùng LinkedHashMap để giữ thứ tự ưu tiên của FTS và loại bỏ trùng lặp
        Map<Long, Job> combinedResults = new LinkedHashMap<>();
        ftsResults.forEach(job -> combinedResults.put(job.getId(), job));
        skillResults.forEach(job -> combinedResults.putIfAbsent(job.getId(), job));

        return combinedResults.values().stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * Helper method để trích xuất keywords từ CV và mô tả.
     */
    private Set<String> extractKeywords(String cvText, String skillsDescription) {
        Set<String> keywords = new HashSet<>();
        String combinedText = (cvText != null ? cvText : "") + " "
                + (skillsDescription != null ? skillsDescription : "");
        if (combinedText.trim().isEmpty())
            return keywords;

        String[] words = combinedText.toLowerCase().split("[\\s,;\\n\\t()./]+");
        for (String word : words) {
            // Lọc bỏ các từ quá ngắn hoặc không có nghĩa
            if (word.length() > 1 && word.matches("[a-z0-9+#-]*[a-z0-9]")) {
                keywords.add(word);
            }
        }
        return keywords;
    }

    /**
     * ĐÃ CẬP NHẬT: Gửi thông tin ứng viên dưới dạng text thay vì file
     */
    private List<GeminiJobScoreResponse> rankJobsWithGemini(
            String skillsDescription, byte[] cvFileBytes, String cvFileName, List<Job> jobs) {

        List<Map<String, Object>> parts = new ArrayList<>();

        String initialPrompt = "You are an expert career advisor. Based on the candidate's skills and/or resume text, please evaluate the following job openings. "
                + "Prioritize the information in the resume text if it exists.\n\n"
                + "Candidate's Information:";
        parts.add(Map.of("text", initialPrompt));

        // Thêm thông tin ứng viên (text hoặc trích xuất text từ file)
        if (cvFileBytes != null) {
            try {
                String cvText = fileService.extractTextFromBytes(cvFileBytes);
                parts.add(Map.of("text", "Candidate Resume Text:\n" + cvText));
            } catch (IOException e) {
                System.err.println("Could not extract text from CV for job finding: " + e.getMessage());
            }
            if (skillsDescription != null && !skillsDescription.trim().isEmpty()) {
                parts.add(Map.of("text", "Additional skills summary: " + skillsDescription));
            }
        } else if (skillsDescription != null && !skillsDescription.trim().isEmpty()) {
            parts.add(Map.of("text", skillsDescription));
        }

        try {
            String jobsJson = objectMapper.writeValueAsString(
                    jobs.stream().map(jobService::convertToResFetchJobDTO).collect(Collectors.toList()));
            parts.add(Map.of("text", "\n\nHere are the job openings to evaluate:\n" + jobsJson));
        } catch (Exception e) {
            System.err.println("Error converting jobs to JSON: " + e.getMessage());
        }

        String finalPrompt = "\n\nAfter analyzing all jobs, return a JSON array of objects. Each object must have 'jobId' and 'score' (0-100). "
                + "Rank them from highest score to lowest. Only include jobs that are a good match and have a score greater than 0. "
                + "Exclude jobs with a score of 0, such as those where the candidate's skills do not match the job requirements or the job location is incompatible with the candidate's preferences. "
                + "Response (JSON array of objects only):";
        parts.add(Map.of("text", finalPrompt));

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(
                    Map.of("contents", Collections.singletonList(Map.of("parts", parts))), headers);
            String url = geminiApiUrl + "?key=" + geminiApiKey;
            String response = restTemplate.postForObject(url, entity, String.class);
            JsonNode root = objectMapper.readTree(response);
            String textResponse = root.path("candidates").path(0).path("content").path("parts").path(0).path("text")
                    .asText();
            String cleanedJson = textResponse.replace("```json", "").replace("```", "").trim();

            return objectMapper.readValue(cleanedJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, GeminiJobScoreResponse.class));
        } catch (Exception e) {
            System.err.println("Error calling Gemini API for finding jobs: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    // Helper class để parse response
    @Getter
    @Setter
    private static class GeminiJobScoreResponse {
        private long jobId;
        private int score;
    }
}
