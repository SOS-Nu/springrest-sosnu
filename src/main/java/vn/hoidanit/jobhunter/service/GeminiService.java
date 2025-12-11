package vn.hoidanit.jobhunter.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import vn.hoidanit.jobhunter.domain.entity.Job;
import vn.hoidanit.jobhunter.domain.entity.User;
import vn.hoidanit.jobhunter.domain.response.ResCandidateWithScoreDTO;
import vn.hoidanit.jobhunter.domain.response.ResFindCandidatesDTO;
import vn.hoidanit.jobhunter.domain.response.ResInitiateCandidateSearchDTO;
import vn.hoidanit.jobhunter.domain.response.ResUserDetailDTO;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.domain.response.ai.CandidateSearchState;
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

@Service
@Slf4j
public class GeminiService {
    private static final int CANDIDATE_CHUNK_SIZE = 50;

    private static final int CHUNK_SIZE = 200;

    private static final String GEMINI_MODEL = "gemini-2.5-flash";

    private static final GenerateContentConfig FAST_JSON_CONFIG = GenerateContentConfig.builder()
            .temperature(0.2f)
            .topP(0.95f)
            .maxOutputTokens(20000)
            .responseMimeType("application/json")
            .build();

    private final Client geminiClient;
    private final RestTemplate restTemplate;
    private final UserService userService;
    private final FileService fileService;
    private final ObjectMapper objectMapper;
    private final JobRepository jobRepository;
    private final JobService jobService;
    private final UserRepository userRepository;
    private final CacheManager cacheManager;
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    public GeminiService(RestTemplate restTemplate, UserService userService, FileService fileService,
            ObjectMapper objectMapper, JobRepository jobRepository, JobService jobService, CacheManager cacheManager,
            UserRepository userRepository, Client geminiClient) {
        this.restTemplate = restTemplate;
        this.userService = userService;
        this.fileService = fileService;
        this.objectMapper = objectMapper;
        this.jobRepository = jobRepository;
        this.jobService = jobService;
        this.userRepository = userRepository;
        this.cacheManager = cacheManager;
        this.geminiClient = geminiClient;
    }
    // ================= START: LOGIC TÌM KIẾM ỨNG VIÊN MỚI =================

    // #region Find Candidate Users
    /**
     * Khởi tạo một phiên tìm kiếm ứng viên mới.
     * Xử lý TOÀN BỘ ứng viên tiềm năng, gọi AI, sắp xếp, và lưu vào cache.
     * Trả về trang đầu tiên của kết quả.
     */
    public ResInitiateCandidateSearchDTO initiateCandidateSearch(String jobDescription, Pageable pageable)
            throws IdInvalidException {
        System.out.println("LOG: >>> [START] Initiating new ON-DEMAND candidate search...");
        String keywords = extractKeywordsWithGemini(jobDescription);

        // <<< LOG MỚI >>>
        // Log này để kiểm tra nội dung text thô nhận được từ controller.
        System.out.println("LOG-DEBUG: Received raw job description text (length: "
                + (jobDescription != null ? jobDescription.length() : 0) + ")");

        // <<< LOG MỚI >>>
        // Log này cho thấy chuỗi cuối cùng được truyền vào câu lệnh FTS. Nếu nó rỗng,
        // FTS sẽ không tìm thấy gì.
        System.out.println("LOG-DEBUG: Keywords string for FTS query: '" + keywords + "'");

        final int PRE_FILTER_LIMIT = 1000;
        List<User> potentialUsers = this.userRepository.preFilterCandidatesByKeywords(keywords, PRE_FILTER_LIMIT);
        System.out.println("LOG: Found " + potentialUsers.size() + " potential candidates after FTS pre-filtering.");

        List<Long> potentialUserIds = potentialUsers.stream().map(User::getId).collect(Collectors.toList());
        System.out.println("LOG: Found " + potentialUserIds.size() + " potential candidates after FTS pre-filtering.");

        // Tạo State và searchId
        CandidateSearchState state = new CandidateSearchState();
        state.setPotentialUserIds(potentialUserIds);
        state.setJobDescription(jobDescription);
        String searchId = "candidate_search:" + SecurityUtil.hash(jobDescription + System.currentTimeMillis());

        // Chỉ xử lý đủ cho trang đầu tiên
        int targetSize = pageable.getPageSize();
        processCandidateChunks(state, targetSize);

        // Lưu trạng thái ban đầu vào cache
        Cache cache = cacheManager.getCache("jobMatches");
        cache.put(searchId, state);
        System.out.println("LOG: Initial state saved to cache with searchId: " + searchId);

        // Lấy kết quả cho trang đầu
        List<ResCandidateWithScoreDTO> paginatedResult = state.getFoundCandidates().stream()
                .limit(pageable.getPageSize()).collect(Collectors.toList());

        ResultPaginationDTO.Meta meta = createCandidateMeta(state, pageable);

        System.out.println("LOG: <<< [END] On-demand candidate search initiated. Returning first page.");
        return new ResInitiateCandidateSearchDTO(paginatedResult, meta, searchId);
    }

    /**
     * Lấy các trang kết quả tiếp theo, xử lý thêm ứng viên nếu cần.
     */
    public ResFindCandidatesDTO getCandidateSearchResults(String searchId, Pageable pageable)
            throws IdInvalidException {
        System.out.println("LOG: >>> [START] Fetching candidate search results for searchId: " + searchId);
        Cache cache = cacheManager.getCache("jobMatches");
        CandidateSearchState state = cache.get(searchId, CandidateSearchState.class);

        if (state == null) {
            throw new IdInvalidException("Phiên tìm kiếm ứng viên không tồn tại hoặc đã hết hạn.");
        }

        // Tính toán số lượng ứng viên cần có để hiển thị trang hiện tại
        int targetSize = (pageable.getPageNumber() + 1) * pageable.getPageSize();

        // Nếu số ứng viên đã tìm thấy không đủ và vẫn còn ứng viên tiềm năng -> xử lý
        // tiếp
        if (state.getFoundCandidates().size() < targetSize && !state.isFullyProcessed()) {
            System.out.println("LOG: Not enough candidates for page " + (pageable.getPageNumber() + 1)
                    + ". Processing more chunks...");
            processCandidateChunks(state, targetSize);
            // Cập nhật lại trạng thái mới vào cache
            cache.put(searchId, state);
            System.out.println("LOG: State updated in cache after processing more chunks.");
        }

        // Phân trang trên danh sách kết quả hiện có
        List<ResCandidateWithScoreDTO> paginatedResult = state.getFoundCandidates().stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .collect(Collectors.toList());

        ResultPaginationDTO.Meta meta = createCandidateMeta(state, pageable);

        System.out.println("LOG: <<< [END] Returning page " + meta.getPage() + " of candidate results.");
        return new ResFindCandidatesDTO(paginatedResult, meta);
    }

    /**
     * PHƯƠNG THỨC CỐT LÕI MỚI: Xử lý các cụm Ứng viên theo yêu cầu
     */
    private void processCandidateChunks(CandidateSearchState state, int targetFoundSize) {
        // Tiếp tục xử lý chừng nào chưa đủ kết quả VÀ vẫn còn user tiềm năng
        while (state.getFoundCandidates().size() < targetFoundSize && !state.isFullyProcessed()) {
            int startIndex = state.getLastProcessedIndex();
            if (startIndex >= state.getPotentialUserIds().size()) {
                state.setFullyProcessed(true); // Đánh dấu đã xử lý hết
                System.out.println("LOG: All potential candidates have been processed.");
                break;
            }

            int endIndex = Math.min(startIndex + CANDIDATE_CHUNK_SIZE, state.getPotentialUserIds().size());
            List<Long> currentChunkIds = state.getPotentialUserIds().subList(startIndex, endIndex);

            // Lấy thông tin user cho cụm hiện tại
            List<User> usersToProcess = this.userRepository.findAllById(currentChunkIds);
            List<ResUserDetailDTO> chunkUserDetails = usersToProcess.stream()
                    .map(ResUserDetailDTO::convertToDTO)
                    .collect(Collectors.toList());

            if (!chunkUserDetails.isEmpty()) {
                System.out.println("LOG: Processing chunk from index " + startIndex + " to " + endIndex);

                // Gọi AI để chấm điểm cho cụm này
                List<GeminiScoreResponse> rankedUsers = rankUsersWithGemini(state.getJobDescription(),
                        chunkUserDetails);

                Map<Long, ResUserDetailDTO> userMap = chunkUserDetails.stream()
                        .collect(Collectors.toMap(ResUserDetailDTO::getId, Function.identity()));

                // Thêm kết quả vào danh sách, không sắp xếp lại
                for (GeminiScoreResponse rankedUser : rankedUsers) {
                    ResUserDetailDTO userDetail = userMap.get(rankedUser.getUserId());
                    if (userDetail != null) {
                        state.getFoundCandidates().add(new ResCandidateWithScoreDTO(rankedUser.getScore(), userDetail));
                    }
                }
            }

            // Cập nhật lại con trỏ vị trí đã xử lý
            state.setLastProcessedIndex(endIndex);

            // =================================================================
            // >>> FIX: THÊM KIỂM TRA CUỐI CÙNG SAU VÒNG LẶP <<<
            // =================================================================
            if (state.getLastProcessedIndex() >= state.getPotentialUserIds().size()) {
                state.setFullyProcessed(true);
            }

            // Thêm độ trễ để tránh lỗi 429
            if (!state.isFullyProcessed() && state.getFoundCandidates().size() < targetFoundSize) {
                try {
                    System.out.println("LOG: Delaying for 1 second before next API call...");
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("LOG-ERROR: Thread was interrupted during delay.");
                }
            }
        }
    }

    /**
     * Helper để tạo meta, tránh lặp code
     */
    private ResultPaginationDTO.Meta createCandidateMeta(CandidateSearchState state, Pageable pageable) {
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        long totalFound = state.getFoundCandidates().size();

        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());

        // Total và Pages sẽ tăng dần khi có thêm kết quả
        meta.setTotal(totalFound);
        meta.setPages((int) Math.ceil((double) totalFound / pageable.getPageSize()));

        // =================================================================
        // >>> FIX QUAN TRỌNG: ĐẢM BẢO `hasMore` LUÔN DỰA VÀO ĐÂY <<<
        // Dòng này sẽ tính toán `hasMore` một cách chính xác sau khi
        // lỗi ở Phần 1 đã được sửa.
        // =================================================================
        meta.setHasMore(!state.isFullyProcessed());

        return meta;
    }

    // #endregion

    // #region find user gemini
    /**
     * ĐÃ CẬP NHẬT: Gửi nội dung text của CV thay vì file base64
     */
    private List<GeminiScoreResponse> rankUsersWithGemini(String jobDescription, List<ResUserDetailDTO> users) {
        log.info(">>> Calling Gemini SDK to rank {} users", users.size());

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(
                "You are an expert HR assistant. Based on the following job description, analyze each candidate. ");
        promptBuilder.append("Prioritize the resume text. \n\n");
        promptBuilder.append("Job Description:\n\"").append(jobDescription).append("\"\n\n");

        for (ResUserDetailDTO userDetail : users) {
            try {
                User userEntity = this.userService.fetchUserById(userDetail.getId());
                if (userEntity == null)
                    continue;

                StringBuilder combinedResumeText = new StringBuilder();

                // 1. Online Resume
                if (userEntity.getOnlineResume() != null) {
                    combinedResumeText.append("--- ONLINE RESUME ---\n")
                            .append(buildTextFromOnlineResume(userEntity)).append("\n\n");
                }

                // 2. Main Resume (File)
                String mainResumeFileName = userEntity.getMainResume();
                if (mainResumeFileName != null && !mainResumeFileName.isEmpty()) {
                    try {
                        String fileResumeText = fileService.extractTextFromStoredFile(mainResumeFileName, "resumes");
                        if (fileResumeText != null && !fileResumeText.trim().isEmpty()) {
                            combinedResumeText.append("--- UPLOADED CV FILE ---\n").append(fileResumeText).append("\n");
                        }
                    } catch (IOException | URISyntaxException e) {
                        log.warn("Could not extract text from file {} for user {}: {}", mainResumeFileName,
                                userEntity.getId(), e.getMessage());
                    }
                }

                // Append to prompt
                promptBuilder.append("Candidate ID: ").append(userDetail.getId()).append("\n");
                // Clone DTO to remove circular refs or unnecessary fields if needed, or just
                // use minimal JSON
                userDetail.setMainResume(null); // Clean for JSON
                promptBuilder.append("Metadata: ").append(objectMapper.writeValueAsString(userDetail)).append("\n");
                promptBuilder.append("Resume Content:\n").append(combinedResumeText).append("\n");
                promptBuilder.append("--------------------------------------------------\n");

            } catch (Exception e) {
                log.error("Error preparing data for user ID {}: {}", userDetail.getId(), e.getMessage());
            }
        }

        promptBuilder.append(
                "\n\nAfter analyzing, return a JSON array. Objects must have 'userId' (number) and 'score' (0-100). ");
        promptBuilder.append("Filter score > 50. Sort desc. Format: [{\"userId\": 1, \"score\": 90}, ...]");

        return callGeminiAndParseList(promptBuilder.toString(), GeminiScoreResponse.class);
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

    // #endregion

    // ================= END: LOGIC TÌM KIẾM ỨNG VIÊN MỚI =================

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

    public ResInitiateSearchDTO initiateJobSearch(
            String skillsDescription, byte[] cvFileBytes, Pageable pageable)
            throws IdInvalidException, IOException {

        String cvText = (cvFileBytes != null) ? fileService.extractTextFromBytes(cvFileBytes) : "";
        String fullInputText = (cvText + " " + skillsDescription).trim();

        String cacheKey = "find_jobs:" + SecurityUtil.hash(fullInputText);
        Cache cache = cacheManager.getCache("jobMatches");

        // <<< THAY ĐỔI LỚN Ở ĐÂY >>>
        // 1. Dùng AI trích xuất keyword từ CV + Skill desc
        String keywords = extractKeywordsWithGemini(fullInputText);

        // 2. Tách chuỗi keyword thành Set để dùng cho hàm preFilterActiveJobs cũ
        // (Hoặc bạn có thể viết lại preFilterActiveJobs để nhận String keywords trực
        // tiếp cũng được)
        Set<String> keywordSet = new HashSet<>(Arrays.asList(keywords.split("\\s+")));

        final int PRE_FILTER_LIMIT = 1000;
        // 3. Gọi DB lọc
        List<Job> potentialJobs = preFilterActiveJobs(keywordSet, PRE_FILTER_LIMIT);
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
     * Helper method để trích xuất keywords từ CV và mô tả.
     */
    /**
     * BƯỚC 1: Dùng AI trích xuất keywords quan trọng từ văn bản thô.
     * Mục đích: Tạo đầu vào chất lượng cao cho MySQL Full-Text Search.
     */
    private String extractKeywordsWithGemini(String originalText) {
        if (originalText == null || originalText.trim().isEmpty()) {
            return "";
        }

        // Log kiểm tra input xem có bị lỗi font từ Controller xuống không
        System.out.println("LOG-DEBUG: Raw input for Gemini extraction: " + originalText);

        // Prompt mới: "Expand" thay vì chỉ "Extract"
        String prompt = "You are an expert technical recruiter and search engine optimizer. " +
                "Your task is to generate a list of relevant keywords for a Full-Text Search engine based on the user input. "
                +
                "\n\nRULES:" +
                "\n1. Analyze the input text: '" + originalText + "'" +
                "\n2. If the input is a Job Description or Resume (long text): Extract top 20 most important technical skills, job titles, and tools."
                +
                "\n3. **CRITICAL**: If the input is SHORT (e.g., 'Kỹ sư AI', 'Java Dev', 'Marketing'): " +
                "   You MUST EXPAND it by generating synonyms, related technical skills, and English translations. " +
                "   (Example: Input 'Kỹ sư AI' -> Output: 'Artificial Intelligence Machine Learning Deep Learning Python NLP Data Scientist Computer Vision django Data Science,....')."
                +
                "\n4. Remove stop words (and, or, the, at, ...). Output must be a single space-separated string." +
                "\n5. Return ONLY a JSON object: {\"keywords\": \"keyword1 keyword2 ...\"}." +
                "\n\nResponse:";

        try {
            // Dùng FAST_JSON_CONFIG để trả về nhanh
            GenerateContentResponse response = geminiClient.models.generateContent(
                    GEMINI_MODEL,
                    prompt,
                    FAST_JSON_CONFIG);

            String cleanedJson = cleanJson(response.text());
            KeywordResponse res = objectMapper.readValue(cleanedJson, KeywordResponse.class);

            log.info(">>> Gemini Expanded Keywords: {}", res.getKeywords());
            return res.getKeywords();
        } catch (Exception e) {
            log.error("Error extracting keywords with AI: {}", e.getMessage());
            // Fallback: Nếu AI lỗi, trả về nguyên gốc (đã chuẩn hóa)
            return originalText;
        }
    }

    private Set<String> extractKeywords(String cvText, String skillsDescription) {
        Set<String> keywords = new HashSet<>();
        String combinedText = (cvText != null ? cvText : "") + " "
                + (skillsDescription != null ? skillsDescription : "");
        if (combinedText.trim().isEmpty()) {
            return keywords;
        }

        String cleanedText = combinedText.toLowerCase()
                .replaceAll("[^a-zA-Z0-9+#\\s-]", " ");

        // <<< LOG MỚI >>>
        // Log này để xem văn bản sau khi làm sạch có còn giữ lại các từ khóa mong muốn
        // không.
        System.out.println("LOG-DEBUG: Text after cleaning: '" + cleanedText.trim().replaceAll("\\s+", " ") + "'");

        String[] words = cleanedText.split("\\s+");

        for (String word : words) {
            if (word.length() > 1) {
                keywords.add(word);
            }
        }
        return keywords;
    }

    @Getter
    @Setter
    private static class KeywordResponse {
        private String keywords;
    }

    /**
     * ĐÃ CẬP NHẬT: Gửi thông tin ứng viên dưới dạng text thay vì file
     */
    private List<GeminiJobScoreResponse> rankJobsWithGemini(String skillsDescription, String cvText, List<Job> jobs) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an expert career advisor. Evaluate these jobs for the candidate.\n");

        boolean hasCvText = (cvText != null && !cvText.trim().isEmpty());
        if (hasCvText) {
            prompt.append("Candidate Resume:\n").append(cvText).append("\n");
        }
        if (skillsDescription != null && !skillsDescription.trim().isEmpty()) {
            prompt.append("Skills Summary: ").append(skillsDescription).append("\n");
        }

        try {
            String jobsJson = convertJobsToJson(jobs); // Dùng lại hàm helper cũ
            prompt.append("\nJobs to evaluate (JSON):\n").append(jobsJson);
        } catch (Exception e) {
            log.error("Error converting jobs to JSON", e);
            return Collections.emptyList();
        }

        prompt.append(
                "\n\nReturn a JSON array: [{\"jobId\": number, \"score\": number}]. Rank high to low. Exclude 0 score.");

        return callGeminiAndParseList(prompt.toString(), GeminiJobScoreResponse.class);
    }

    // =========================================================================
    // HELPER METHODS (CLEAN ARCHITECTURE & REUSE)
    // =========================================================================

    /**
     * Generic method to call Gemini and parse a JSON List response
     */
    private <T> List<T> callGeminiAndParseList(String prompt, Class<T> clazz) {
        try {
            // CẬP NHẬT Ở ĐÂY: Thêm FAST_JSON_CONFIG
            GenerateContentResponse response = geminiClient.models.generateContent(
                    GEMINI_MODEL,
                    prompt,
                    FAST_JSON_CONFIG // <--- Thay null bằng config
            );

            String textResponse = response.text();
            String cleanedJson = cleanJson(textResponse);

            return objectMapper.readValue(cleanedJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            log.error("Gemini SDK Call Failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Utility làm sạch chuỗi JSON trả về từ AI (thường bị bao bởi ```json ... ```)
     */
    private String cleanJson(String rawText) {
        if (rawText == null)
            return "{}";
        String cleaned = rawText.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    // PHƯƠNG THỨC MỚI CỐT LÕI
    public ResCvEvaluationDTO evaluateCandidateCv(MultipartFile cvFile, String language)
            throws IdInvalidException, IOException {
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(() -> new IdInvalidException("User not found"));
        User currentUser = this.userRepository.findByEmail(email);

        String cvAsText;
        if (cvFile != null && !cvFile.isEmpty()) {
            cvAsText = fileService.extractTextFromBytes(cvFile.getBytes());
        } else {
            cvAsText = buildTextFromOnlineResume(currentUser);
        }

        if (cvAsText == null || cvAsText.trim().isEmpty()) {
            throw new IOException("CV content is empty.");
        }

        // Lấy 50 job gần nhất để tiết kiệm token
        Pageable pageable = PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Job> recentJobs = this.jobRepository.findAll((root, query, cb) -> cb.isTrue(root.get("active")), pageable)
                .getContent();
        String jobsJson = convertJobsToJson(recentJobs);

        String prompt = buildCvEvaluationPrompt(cvAsText, jobsJson, language);

        try {
            // CẬP NHẬT Ở ĐÂY: Thêm FAST_JSON_CONFIG
            GenerateContentResponse response = geminiClient.models.generateContent(
                    GEMINI_MODEL,
                    prompt,
                    FAST_JSON_CONFIG);

            String cleanedJson = cleanJson(response.text());
            return objectMapper.readValue(cleanedJson, ResCvEvaluationDTO.class);
        } catch (Exception e) {
            log.error("Error calling Gemini SDK (evaluateCv): {}", e.getMessage());
            throw new IOException("Failed to analyze CV with AI.", e);
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

    public int scoreCvAgainstJob(Job job, byte[] cvFileBytes, String cvFileName) {
        String jobDetails = String.format("Job Title: %s\nLocation: %s\nLevel: %s\nDescription: %s",
                job.getName(), job.getLocation(), job.getLevel(), job.getDescription());

        String cvText;
        try {
            cvText = fileService.extractTextFromBytes(cvFileBytes);
        } catch (IOException e) {
            log.error("Error extracting text from CV bytes: {}", e.getMessage());
            return 0;
        }

        if (cvText == null || cvText.trim().isEmpty())
            return 0;

        String prompt = "As an expert HR, evaluate this resume against the job description. " +
                "Return ONLY a JSON object: {\"score\": number (0-100)}. \n\n" +
                "Job Description:\n" + jobDetails + "\n\n" +
                "Candidate's Resume Text:\n" + cvText;

        try {
            // CẬP NHẬT Ở ĐÂY: Thêm FAST_JSON_CONFIG
            GenerateContentResponse response = geminiClient.models.generateContent(
                    GEMINI_MODEL,
                    prompt,
                    FAST_JSON_CONFIG // <--- Thay null bằng config
            );

            // Khi dùng mode JSON, đôi khi AI không trả về markdown ```json nữa
            // nhưng vẫn nên giữ cleanJson để an toàn tuyệt đối
            String cleanedJson = cleanJson(response.text());
            JsonNode root = objectMapper.readTree(cleanedJson);
            return root.path("score").asInt(0);
        } catch (Exception e) {
            log.error("Error calling Gemini SDK (scoreCv): {}", e.getMessage());
            return 0;
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
