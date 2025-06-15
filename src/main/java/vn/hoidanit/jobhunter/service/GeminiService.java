package vn.hoidanit.jobhunter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    public GeminiService(RestTemplate restTemplate, UserService userService, FileService fileService, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.userService = userService;
        this.fileService = fileService;
        this.objectMapper = objectMapper;
    }


    public ResFindCandidatesDTO findCandidates(String jobDescription) throws IdInvalidException {
        List<ResCandidateWithScoreDTO> suitableCandidates = new ArrayList<>();
        ResultPaginationDTO.Meta lastMeta = null;
        int currentPage = 0;
        final int PAGE_SIZE = 10;
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
                if (suitableCandidates.size() >= TARGET_CANDIDATES) break;

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

    private List<GeminiScoreResponse> rankUsersWithGemini(String jobDescription, List<ResUserDetailDTO> users) {
        // ... (phần code xây dựng `parts` cho request giữ nguyên như trước)
        List<Map<String, Object>> parts = new ArrayList<>();
        
        String initialPrompt = "You are an expert HR assistant. Based on the following job description, please analyze each candidate. " +
                               "Each candidate's information is provided as a combination of structured JSON data and possibly an attached resume file. " +
                               "Prioritize the information in the attached resume file if it exists. " +
                               "\n\nJob Description:\n\"" + jobDescription + "\"\n\n";
        parts.add(Map.of("text", initialPrompt));

        for (ResUserDetailDTO user : users) {
             try {
                String resumeFileName = user.getMainResume();
                user.setMainResume(null);
                String userJson = objectMapper.writeValueAsString(user);
                user.setMainResume(resumeFileName);
                byte[] fileBytes = null;
                if (resumeFileName != null && !resumeFileName.isEmpty()) {
                    fileBytes = fileService.readFileAsBytes(resumeFileName, "resumes");
                }

                if (fileBytes != null) {
                    String encodedFile = Base64.getEncoder().encodeToString(fileBytes);
                    parts.add(Map.of("text", "Candidate data: " + userJson));
                    parts.add(Map.of("inlineData", Map.of(
                        "mimeType", getMimeType(resumeFileName),
                        "data", encodedFile
                    )));
                } else {
                    parts.add(Map.of("text", "Candidate data (no resume file): " + userJson));
                }
            } catch (IOException | URISyntaxException e) {
                System.err.println("Error processing user data or file for user ID " + user.getId() + ": " + e.getMessage());
            }
        }
        
        // CẬP NHẬT PROMPT CUỐI CÙNG
        String finalPrompt = "\n\nAfter analyzing all candidates, return a JSON array of objects. Each object must have two keys: 'userId' (a number) and 'score' (a number from 0 to 100 representing how well they match the job description). " +
                             "Only include candidates who are a good match. Rank the array from the highest score to the lowest. " +
                             "Response (JSON array of objects only):";
        parts.add(Map.of("text", finalPrompt));
        
        try {
            // ... (phần code gửi request tới Gemini giữ nguyên)
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
            String textResponse = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText();
            String cleanedJson = textResponse.replace("```json", "").replace("```", "").trim();

            // CẬP NHẬT PARSING RESPONSE
            return objectMapper.readValue(cleanedJson, objectMapper.getTypeFactory().constructCollectionType(List.class, GeminiScoreResponse.class));

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
     * @param job Thông tin chi tiết về công việc.
     * @param cvFileBytes Nội dung file CV dưới dạng byte array.
     * @param cvFileName Tên file CV để xác định mime-type.
     * @return Điểm số từ 0-100.
     */
    public int scoreCvAgainstJob(Job job, byte[] cvFileBytes, String cvFileName) {
        // Xây dựng mô tả công việc từ các trường
        String jobDetails = String.format(
            "Job Title: %s\nLocation: %s\nLevel: %s\nDescription: %s",
            job.getName(), job.getLocation(), job.getLevel(), job.getDescription()
        );

        // Xây dựng prompt
        List<Map<String, Object>> parts = new ArrayList<>();
        
        String promptText = "As an expert HR, please evaluate the following resume against the job description. " +
                            "Provide a suitability score on a scale of 0 to 100. " +
                            "Return ONLY a JSON object with a single key 'score'. Example: {\"score\": 95}\n\n" +
                            "Job Description:\n" + jobDetails + "\n\n" +
                            "Candidate's Resume:";
        parts.add(Map.of("text", promptText));

        // Thêm dữ liệu file CV
        String encodedFile = Base64.getEncoder().encodeToString(cvFileBytes);
        parts.add(Map.of("inlineData", Map.of(
            "mimeType", getMimeType(cvFileName),
            "data", encodedFile
        )));

        // Gửi request tới Gemini
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> content = Map.of("parts", parts);
            Map<String, Object> requestBody = Map.of("contents", Collections.singletonList(content));
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            String url = geminiApiUrl + "?key=" + geminiApiKey;
            String response = restTemplate.postForObject(url, entity, String.class);

            // Parse response
            JsonNode root = objectMapper.readTree(response);
            String textResponse = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText();
            String cleanedJson = textResponse.replace("```json", "").replace("```", "").trim();
            JsonNode scoreNode = objectMapper.readTree(cleanedJson);
            
            return scoreNode.path("score").asInt(0); // Trả về 0 nếu không tìm thấy score

        } catch (Exception e) {
            System.err.println("Error calling Gemini API for scoring: " + e.getMessage());
            return 0; // Trả về 0 nếu có lỗi
        }
    }
}
