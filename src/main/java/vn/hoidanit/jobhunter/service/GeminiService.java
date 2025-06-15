package vn.hoidanit.jobhunter.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import vn.hoidanit.jobhunter.domain.response.ResFindCandidatesDTO;
import vn.hoidanit.jobhunter.domain.response.ResUserDetailDTO;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.util.error.IdInvalidException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GeminiService {

    private final RestTemplate restTemplate;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    public GeminiService(RestTemplate restTemplate, UserService userService, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    public ResFindCandidatesDTO findCandidates(String jobDescription) throws IdInvalidException {
        List<ResUserDetailDTO> suitableCandidates = new ArrayList<>();
        ResultPaginationDTO.Meta lastMeta = null;
        int currentPage = 0; // Bắt đầu từ trang 0 cho Pageable
        final int PAGE_SIZE = 20;
        final int TARGET_CANDIDATES = 10;

        // Lặp cho đến khi tìm đủ ứng viên hoặc hết dữ liệu
        while (suitableCandidates.size() < TARGET_CANDIDATES) {
            Pageable pageable = PageRequest.of(currentPage, PAGE_SIZE);
            ResultPaginationDTO paginatedUsers = userService.fetchAllUserDetails(null, pageable);
            lastMeta = paginatedUsers.getMeta();

            @SuppressWarnings("unchecked")
            List<ResUserDetailDTO> currentUsers = (List<ResUserDetailDTO>) paginatedUsers.getResult();
            if (currentUsers.isEmpty()) {
                break; // Hết người dùng để kiểm tra
            }

            // Gửi dữ liệu tới Gemini để phân tích
            List<Long> rankedUserIds = rankUsersWithGemini(jobDescription, currentUsers);

            // Thêm các ứng viên phù hợp vào danh sách kết quả
            for (Long userId : rankedUserIds) {
                if (suitableCandidates.size() >= TARGET_CANDIDATES) break;
                currentUsers.stream()
                        .filter(u -> u.getId() == userId)
                        .findFirst()
                        .ifPresent(suitableCandidates::add);
            }

            currentPage++;
        }

        return new ResFindCandidatesDTO(suitableCandidates, lastMeta);
    }

    private List<Long> rankUsersWithGemini(String jobDescription, List<ResUserDetailDTO> users) {
        try {
            String usersJson = objectMapper.writeValueAsString(users);
            String prompt = "You are an expert HR assistant. Based on the following job description, please analyze the list of candidates provided in JSON format. " +
                    "Return a JSON array containing the user IDs of the candidates who are the best fit, ranked from most to least suitable. " +
                    "Only return the IDs of candidates who are a good match.\n\n" +
                    "Job Description:\n\"" + jobDescription + "\"\n\n" +
                    "Candidates List:\n" + usersJson + "\n\n" +
                    "Response (JSON array of user IDs only):";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", prompt);

            Map<String, Object> content = new HashMap<>();
            content.put("parts", Collections.singletonList(textPart));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", Collections.singletonList(content));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String url = geminiApiUrl + "?key=" + geminiApiKey;
            String response = restTemplate.postForObject(url, entity, String.class);

            // Parse response để lấy text
            JsonNode root = objectMapper.readTree(response);
            String textResponse = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText();
            
            // Text trả về có thể chứa ```json ... ```, cần làm sạch
            String cleanedJson = textResponse.replace("```json", "").replace("```", "").trim();

            return objectMapper.readValue(cleanedJson, objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class));

        } catch (JsonProcessingException e) {
            // Xử lý lỗi JSON
            System.err.println("Error processing JSON for Gemini: " + e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            // Xử lý lỗi API
            System.err.println("Error calling Gemini API: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
