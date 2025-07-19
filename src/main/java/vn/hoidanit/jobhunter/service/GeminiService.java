package vn.hoidanit.jobhunter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
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
import org.springframework.web.multipart.MultipartFile;

import vn.hoidanit.jobhunter.domain.entity.Job;
import vn.hoidanit.jobhunter.domain.entity.User;
import vn.hoidanit.jobhunter.domain.response.ResCandidateWithScoreDTO;
import vn.hoidanit.jobhunter.domain.response.ResFindCandidatesDTO;
import vn.hoidanit.jobhunter.domain.response.ResUserDetailDTO;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.domain.response.ai.ResCvEvaluationDTO;
import vn.hoidanit.jobhunter.domain.response.ai.SearchState;
import vn.hoidanit.jobhunter.domain.response.job.ResFetchJobDTO;
import vn.hoidanit.jobhunter.domain.response.job.ResFindJobsDTO;
import vn.hoidanit.jobhunter.domain.response.job.ResInitiateSearchDTO;
import vn.hoidanit.jobhunter.domain.response.job.ResJobWithScoreDTO;
import vn.hoidanit.jobhunter.repository.JobRepository;
import vn.hoidanit.jobhunter.repository.UserRepository;
import vn.hoidanit.jobhunter.util.SecurityUtil;
import vn.hoidanit.jobhunter.util.error.IdInvalidException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GeminiService {
    private static final int CHUNK_SIZE = 100;

    // ... (Giữ nguyên các biến và constructor)
    private final RestTemplate restTemplate;
    private final UserService userService;
    private final FileService fileService;
    private final ObjectMapper objectMapper;
    private final JobRepository jobRepository; // THÊM REPOSITORY NÀY
    private final JobService jobService; // THÊM SERVICE NÀY
    private final UserRepository userRepository;
    private final CacheManager cacheManager; // <<< THÊM BIẾN NÀY

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    public GeminiService(RestTemplate restTemplate, UserService userService, FileService fileService,
            ObjectMapper objectMapper, JobRepository jobRepository, JobService jobService, CacheManager cacheManager,
            UserRepository userRepository) {
        this.restTemplate = restTemplate;
        this.userService = userService;
        this.fileService = fileService;
        this.objectMapper = objectMapper;
        this.jobRepository = jobRepository;
        this.jobService = jobService;
        this.userRepository = userRepository;
        this.cacheManager = cacheManager;
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

    // public ResFindJobsDTO findJobsForCandidate(
    // String skillsDescription, byte[] cvFileBytes, String cvFileName, Pageable
    // pageable)
    // throws IdInvalidException, IOException {
    //
    // final int PRE_FILTER_LIMIT = 200;
    //
    // String cvText = (cvFileBytes != null) ?
    // fileService.extractTextFromBytes(cvFileBytes) : "";
    //
    // // <<< THAY ĐỔI CỐT LÕI: SỬ DỤNG CACHE >>>
    // // 1. Tạo cache key duy nhất từ nội dung CV và skills
    // String cacheKey = "find_jobs:" + SecurityUtil.hash(cvText +
    // skillsDescription);
    //
    // // 2. Lấy "ngăn" cache có tên "jobMatches"
    // Cache cache = cacheManager.getCache("jobMatches");
    //
    // // 3. Thử lấy kết quả từ cache trước
    // // Sử dụng get(key, type) để tránh phải ép kiểu thủ công
    // List<ResJobWithScoreDTO> allRankedJobs = cache.get(cacheKey, List.class);
    //
    // // 4. KIỂM TRA CACHE
    // if (allRankedJobs == null) {
    // // ---- CACHE MISS (Không có trong cache) ----
    // System.out.println("LOG: Cache miss! Calling AI for key: " + cacheKey);
    //
    // // Thực hiện logic tìm kiếm và gọi AI như cũ
    // Set<String> keywords = extractKeywords(cvText, skillsDescription);
    // List<Job> potentialJobs = preFilterActiveJobs(keywords, PRE_FILTER_LIMIT);
    //
    // if (potentialJobs.isEmpty()) {
    // return new ResFindJobsDTO(Collections.emptyList(), new
    // ResultPaginationDTO.Meta());
    // }
    //
    // List<GeminiJobScoreResponse> rankedJobScores = rankJobsWithGemini(
    // skillsDescription, cvText, potentialJobs);
    //
    // Map<Long, Job> jobMap = potentialJobs.stream()
    // .collect(Collectors.toMap(Job::getId, Function.identity()));
    //
    // allRankedJobs = new ArrayList<>(); // Khởi tạo list mới
    // for (GeminiJobScoreResponse rankedJob : rankedJobScores) {
    // Job jobDetail = jobMap.get(rankedJob.getJobId());
    // if (jobDetail != null) {
    // ResFetchJobDTO jobDTO = jobService.convertToResFetchJobDTO(jobDetail);
    // allRankedJobs.add(new ResJobWithScoreDTO(rankedJob.getScore(), jobDTO));
    // }
    // }
    //
    // allRankedJobs.sort(Comparator.comparingInt(ResJobWithScoreDTO::getScore).reversed());
    //
    // // 5. LƯU KẾT QUẢ VÀO CACHE để dùng cho các lần sau
    // cache.put(cacheKey, allRankedJobs);
    //
    // } else {
    // // ---- CACHE HIT (Tìm thấy trong cache) ----
    // System.out.println("LOG: Cache hit! Found data for key: " + cacheKey);
    // }
    //
    // // === PHÂN TRANG KẾT QUẢ TRẢ VỀ (Luôn chạy, dù là cache hit hay miss) ===
    // int start = (int) pageable.getOffset();
    // int end = Math.min((start + pageable.getPageSize()), allRankedJobs.size());
    //
    // List<ResJobWithScoreDTO> paginatedResult = (start >= allRankedJobs.size())
    // ? Collections.emptyList()
    // : allRankedJobs.subList(start, end);
    //
    // // Meta giờ sẽ luôn nhất quán vì `allRankedJobs.size()` không đổi
    // ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
    // meta.setPage(pageable.getPageNumber() + 1);
    // meta.setPageSize(pageable.getPageSize());
    // meta.setPages((int) Math.ceil((double) allRankedJobs.size() /
    // pageable.getPageSize()));
    // meta.setTotal(allRankedJobs.size());
    //
    // return new ResFindJobsDTO(paginatedResult, meta);
    // }

    public ResInitiateSearchDTO initiateJobSearch(
            String skillsDescription, byte[] cvFileBytes, Pageable pageable)
            throws IdInvalidException, IOException {

        String cvText = (cvFileBytes != null) ? fileService.extractTextFromBytes(cvFileBytes) : "";
        String cacheKey = "find_jobs:" + SecurityUtil.hash(cvText + skillsDescription);
        Cache cache = cacheManager.getCache("jobMatches");

        final int PRE_FILTER_LIMIT = 1000;
        Set<String> keywords = extractKeywords(cvText, skillsDescription);
        List<Job> potentialJobs = preFilterActiveJobs(keywords, PRE_FILTER_LIMIT);
        List<Long> potentialJobIds = potentialJobs.stream().map(Job::getId).collect(Collectors.toList());

        SearchState state = new SearchState();
        state.setPotentialJobIds(potentialJobIds);

        // <<< THAY ĐỔI 1: LƯU NGỮ CẢNH VÀO STATE >>>
        state.setSkillsDescription(skillsDescription);
        state.setCvText(cvText);

        int targetSize = pageable.getPageSize();
        processJobChunks(state, targetSize, skillsDescription, cvText);

        cache.put(cacheKey, state);

        List<ResJobWithScoreDTO> paginatedResult = state.getFoundJobs().stream()
                .limit(pageable.getPageSize()).collect(Collectors.toList());

        ResultPaginationDTO.Meta meta = createMeta(state, pageable);

        return new ResInitiateSearchDTO(paginatedResult, meta, cacheKey);
    }

    public ResFindJobsDTO getJobSearchResults(String searchId, Pageable pageable) throws IdInvalidException {
        Cache cache = cacheManager.getCache("jobMatches");
        SearchState state = cache.get(searchId, SearchState.class);

        if (state == null) {
            throw new IdInvalidException("Phiên tìm kiếm không tồn tại hoặc đã hết hạn. Vui lòng thử lại.");
        }

        int targetSize = (pageable.getPageNumber() + 1) * pageable.getPageSize();

        if (state.getFoundJobs().size() < targetSize && !state.isFullyProcessed()) {
            // <<< THAY ĐỔI 2: LẤY LẠI NGỮ CẢNH TỪ STATE >>>
            // Giờ đây chúng ta có thể lấy lại ngữ cảnh một cách an toàn
            String skillsDescription = state.getSkillsDescription();
            String cvText = state.getCvText();

            // Gọi xử lý cụm với ngữ cảnh đã được lấy lại
            processJobChunks(state, targetSize, skillsDescription, cvText);

            // Cập nhật lại trạng thái mới vào cache sau khi đã xử lý thêm
            cache.put(searchId, state);
        }

        List<ResJobWithScoreDTO> paginatedResult = state.getFoundJobs().stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .collect(Collectors.toList());

        ResultPaginationDTO.Meta meta = createMeta(state, pageable);

        return new ResFindJobsDTO(paginatedResult, meta);
    }

    // <<< PHƯƠNG THỨC CỐT LÕI MỚI: Xử lý các cụm Job >>>
    private void processJobChunks(SearchState state, int targetFoundSize, String skillsDescription, String cvText) {
        // Tiếp tục xử lý chừng nào chưa đủ kết quả VÀ vẫn còn job tiềm năng
        while (state.getFoundJobs().size() < targetFoundSize && !state.isFullyProcessed()) {
            int startIndex = state.getLastProcessedIndex();
            if (startIndex >= state.getPotentialJobIds().size()) {
                state.setFullyProcessed(true); // Đánh dấu đã xử lý hết
                break;
            }

            int endIndex = Math.min(startIndex + CHUNK_SIZE, state.getPotentialJobIds().size());

            List<Long> currentChunkIds = state.getPotentialJobIds().subList(startIndex, endIndex);
            List<Job> jobsToProcess = jobRepository.findAllById(currentChunkIds);

            if (!jobsToProcess.isEmpty()) {
                // Gọi AI để chấm điểm cho cụm này.
                // Gemini đã trả về danh sách được sắp xếp cho cụm này.
                List<GeminiJobScoreResponse> rankedJobScores = rankJobsWithGemini(skillsDescription, cvText,
                        jobsToProcess);

                Map<Long, Job> jobMap = jobsToProcess.stream()
                        .collect(Collectors.toMap(Job::getId, Function.identity()));

                for (GeminiJobScoreResponse rankedJob : rankedJobScores) {
                    if (rankedJob.getScore() >= 70) {
                        Job jobDetail = jobMap.get(rankedJob.getJobId());
                        if (jobDetail != null) {
                            ResFetchJobDTO jobDTO = jobService.convertToResFetchJobDTO(jobDetail);
                            // Chỉ cần thêm vào cuối danh sách
                            state.getFoundJobs().add(new ResJobWithScoreDTO(rankedJob.getScore(), jobDTO));
                        }
                    }
                }

                // <<< XÓA DÒNG NÀY >>>
                // state.getFoundJobs().sort(Comparator.comparingInt(ResJobWithScoreDTO::getScore).reversed());
            }

            // Cập nhật lại con trỏ vị trí đã xử lý
            state.setLastProcessedIndex(endIndex);
        }
    }

    // Helper để tạo meta, tránh lặp code
    private ResultPaginationDTO.Meta createMeta(SearchState state, Pageable pageable) {
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        // Total bây giờ là tổng số kết quả đã tìm thấy và lọc qua ngưỡng điểm
        long total = state.getFoundJobs().size();

        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setTotal(total);
        meta.setPages((int) Math.ceil((double) total / pageable.getPageSize()));
        meta.setHasMore(!state.isFullyProcessed());

        return meta;
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
            String skillsDescription, String cvText, List<Job> jobs) {

        List<Map<String, Object>> parts = new ArrayList<>();

        String initialPrompt = "You are an expert career advisor. Based on the candidate's skills and/or resume text, please evaluate the following job openings. "
                + "Prioritize the information in the resume text if it exists.\n\n"
                + "Candidate's Information:";
        parts.add(Map.of("text", initialPrompt));

        // <<< THAY ĐỔI 5: Đơn giản hóa logic, sử dụng trực tiếp cvText >>>
        boolean hasCvText = (cvText != null && !cvText.trim().isEmpty());
        boolean hasSkills = (skillsDescription != null && !skillsDescription.trim().isEmpty());

        if (hasCvText) {
            parts.add(Map.of("text", "Candidate Resume Text:\n" + cvText));
            if (hasSkills) {
                parts.add(Map.of("text", "Additional skills summary: " + skillsDescription));
            }
        } else if (hasSkills) {
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

    // PHƯƠNG THỨC MỚI CỐT LÕI
    public ResCvEvaluationDTO evaluateCandidateCv(MultipartFile cvFile, String language)
            throws IdInvalidException, IOException {
        // 1. Lấy thông tin người dùng hiện tại
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(() -> new IdInvalidException("User not found"));
        User currentUser = this.userRepository.findByEmail(email);
        if (currentUser == null) {
            throw new IdInvalidException("User not found with email: " + email);
        }

        // 2. Lấy nội dung CV
        String cvAsText;
        if (cvFile != null && !cvFile.isEmpty()) {
            // Trường hợp 1: User tải file lên
            cvAsText = fileService.extractTextFromBytes(cvFile.getBytes());
        } else {
            // Trường hợp 2: Dùng online resume
            cvAsText = buildTextFromOnlineResume(currentUser);
        }

        if (cvAsText == null || cvAsText.trim().isEmpty()) {
            throw new IOException("CV content is empty or could not be extracted.");
        }

        // 3. Lấy danh sách công việc từ DB
        // Lấy 100 job mới nhất đang active để AI tham khảo
        Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Job> spec = (root, query, cb) -> cb.isTrue(root.get("active"));
        List<Job> recentJobs = this.jobRepository.findAll(spec, pageable).getContent();

        // Convert jobs to a simplified JSON string
        String jobsJson = convertJobsToJson(recentJobs);

        // 4. Xây dựng Prompt chi tiết cho Gemini
        String prompt = buildCvEvaluationPrompt(cvAsText, jobsJson, language);

        // 5. Gọi API Gemini
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            List<Map<String, Object>> parts = new ArrayList<>();
            parts.add(Map.of("text", prompt));

            Map<String, Object> content = Map.of("parts", parts);
            Map<String, Object> requestBody = Map.of("contents", Collections.singletonList(content));
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String url = geminiApiUrl + "?key=" + geminiApiKey;
            String response = restTemplate.postForObject(url, entity, String.class);

            // 6. Parse kết quả và trả về
            JsonNode root = objectMapper.readTree(response);
            String textResponse = root.path("candidates").get(0).path("content").path("parts").get(0).path("text")
                    .asText();
            String cleanedJson = textResponse.replace("```json", "").replace("```", "").trim();

            return objectMapper.readValue(cleanedJson, ResCvEvaluationDTO.class);

        } catch (Exception e) {
            System.err.println("Error calling Gemini API for CV evaluation: " + e.getMessage());
            // Có thể throw một exception tùy chỉnh ở đây
            throw new IOException("Failed to get response from AI service.", e);
        }
    }

    // Hàm helper để tạo nội dung text từ Online Resume
    private String buildTextFromOnlineResume(User user) {
        if (user.getOnlineResume() == null)
            return "";

        StringBuilder sb = new StringBuilder();
        sb.append("Title: ").append(user.getOnlineResume().getTitle()).append("\n");
        sb.append("Full Name: ").append(user.getOnlineResume().getFullName()).append("\n");
        sb.append("Email: ").append(user.getOnlineResume().getEmail()).append("\n");
        sb.append("Phone: ").append(user.getOnlineResume().getPhone()).append("\n");
        sb.append("Address: ").append(user.getOnlineResume().getAddress()).append("\n\n");

        sb.append("Summary:\n").append(user.getOnlineResume().getSummary()).append("\n\n");

        sb.append("Skills:\n");
        user.getOnlineResume().getSkills().forEach(skill -> sb.append("- ").append(skill.getName()).append("\n"));
        sb.append("\n");

        if (user.getWorkExperiences() != null && !user.getWorkExperiences().isEmpty()) {
            sb.append("Work Experience:\n");
            user.getWorkExperiences().forEach(exp -> {
                sb.append("- Company: ").append(exp.getCompanyName()).append("\n");
                sb.append("  Duration: ").append(exp.getStartDate()).append(" to ").append(exp.getEndDate())
                        .append("\n");
                sb.append("  Description: ").append(exp.getDescription()).append("\n\n");
            });
        }

        return sb.toString();
    }

    // Hàm helper để convert danh sách Job thành chuỗi JSON đơn giản
    private String convertJobsToJson(List<Job> jobs) {
        List<Map<String, Object>> simplifiedJobs = jobs.stream().map(job -> {
            Map<String, Object> map = new HashMap<>();
            map.put("jobId", job.getId());
            map.put("jobTitle", job.getName());
            map.put("companyName", job.getCompany() != null ? job.getCompany().getName() : "N/A");
            map.put("requiredSkills", job.getSkills().stream().map(s -> s.getName()).collect(Collectors.toList()));
            map.put("description", job.getDescription());
            map.put("level", job.getLevel());
            return map;
        }).collect(Collectors.toList());

        try {
            return objectMapper.writeValueAsString(simplifiedJobs);
        } catch (Exception e) {
            return "[]";
        }
    }

    // Hàm helper quan trọng nhất: tạo prompt
    private String buildCvEvaluationPrompt(String cvText, String jobsJson, String language) {
        // Logic để tạo chỉ thị ngôn ngữ dựa trên tham số 'language'
        String languageInstruction = language.equalsIgnoreCase("en")
                ? "You must provide the entire response in English. All keys and values in the JSON must be in English."
                : "Bạn phải cung cấp toàn bộ phản hồi bằng Tiếng Việt. Mọi khóa và giá trị trong JSON phải là Tiếng Việt.";

        String marketContext = language.equalsIgnoreCase("en")
                ? "the Vietnamese IT market"
                : "thị trường IT Việt Nam";

        // Xây dựng prompt hoàn chỉnh
        return "You are an expert HR and career advisor for " + marketContext + ". "
        // Thêm chỉ thị ngôn ngữ vào ngay đầu prompt
                + languageInstruction + " "
                + "Analyze the following CV. "
                + "Provide a comprehensive evaluation in a single, valid JSON object. The JSON object must have the exact following structure: "
                + "{\"overallScore\": number, \"summary\": string, \"strengths\": string[], \"improvements\": [{\"area\": string, \"suggestion\": string}], \"estimatedSalaryRange\": string, \"suggestedRoadmap\": [{\"step\": number, \"action\": string, \"reason\": string}], \"relevantJobs\": [{\"jobId\": number, \"jobTitle\": string, \"companyName\": string, \"matchReason\": string}]}. "
                + "\n\nHere are the evaluation criteria:"
                + "\n1.  **overallScore**: An integer score from 0 to 100 based on clarity, experience, skills, and suitability for the current job market."
                + "\n2.  **summary**: A short, professional summary of the candidate's profile in 2-3 sentences."
                + "\n3.  **strengths**: An array of strings highlighting the candidate's key strengths (e.g., 'Strong experience in microservices architecture', 'Proficient with React and state management')."
                + "\n4.  **improvements**: An array of objects. For each object, 'area' is the section to improve (e.g., 'Project Descriptions', 'Skills Section') and 'suggestion' is a concrete action (e.g., 'Quantify achievements with metrics like 20% performance improvement', 'Add soft skills like teamwork and problem-solving')."
                + "\n5.  **estimatedSalaryRange**: A string representing the estimated appropriate monthly salary range in VND (e.g., '35,000,000 - 45,000,000 VND'). Base this on the skills, experience, and current market rates in Vietnam."
                + "\n6.  **suggestedRoadmap**: A personalized roadmap with 3-5 steps. For each step, 'action' is what to learn or do, and 'reason' explains how it helps them achieve a higher level job or salary."
                + "\n7.  **relevantJobs**: Analyze the list of available jobs provided below. Select the 3-5 most suitable jobs for this candidate. For each, provide 'jobId', 'jobTitle', 'companyName', and a 'matchReason' explaining why it's a good fit."
                + "\n\n---"
                + "\n**CANDIDATE'S CV TEXT:**\n"
                + cvText
                + "\n\n---"
                + "\n**AVAILABLE JOBS FOR REFERENCE:**\n"
                + jobsJson
                + "\n\n---"
                + "\n**RESPONSE (valid JSON object only, no extra text or markdown):**";
    }

    // Helper class để parse response
    @Getter
    @Setter
    private static class GeminiJobScoreResponse {
        private long jobId;
        private int score;
    }
}
