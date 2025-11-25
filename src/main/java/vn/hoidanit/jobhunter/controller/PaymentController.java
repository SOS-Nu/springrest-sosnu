package vn.hoidanit.jobhunter.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.turkraft.springfilter.boot.Filter;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import vn.hoidanit.jobhunter.domain.entity.PaymentHistory;
import vn.hoidanit.jobhunter.domain.entity.User;
import vn.hoidanit.jobhunter.domain.response.PaymentHistoryDTO;
import vn.hoidanit.jobhunter.domain.response.PaymentUrlResponseDTO;
import vn.hoidanit.jobhunter.domain.response.RestResponse;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.repository.PaymentHistoryRepository;
import vn.hoidanit.jobhunter.service.PaymentExportService;
import vn.hoidanit.jobhunter.service.UserService;
import vn.hoidanit.jobhunter.util.SecurityUtil;
import vn.hoidanit.jobhunter.util.annotation.ApiMessage;
import vn.hoidanit.jobhunter.util.error.IdInvalidException;

@RestController
@RequestMapping("/api/v1")
public class PaymentController {

    private final UserService userService;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final PaymentExportService paymentExportService;

    @Value("${vnp.TmnCode}")
    private String vnpTmnCode;

    @Value("${vnp.HashSecret}")
    private String vnpHashSecret;

    @Value("${vnp.Url}")
    private String vnpPaymentUrl;

    @Value("${vnp.ReturnUrl}")
    private String vnpReturnUrl;

    @Value("${vnpay.version}")
    private String vnpVersion;

    @Value("${vnpay.command}")
    private String vnpCommand;

    public PaymentController(UserService userService, PaymentExportService paymentExportService,
            PaymentHistoryRepository paymentHistoryRepository) {
        this.userService = userService;
        this.paymentHistoryRepository = paymentHistoryRepository;
        this.paymentExportService = paymentExportService;
    }

    @PostConstruct
    public void init() {
        System.out.println("PaymentController initialized AFTER injection with vnp_HashSecret: " + vnpHashSecret);
    }

    @PostMapping("/payment/vnpay/create")
    @ApiMessage("Create VNPay payment URL")
    public ResponseEntity<RestResponse<PaymentUrlResponseDTO>> createPaymentUrl(HttpServletRequest request)
            throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(
                () -> new IdInvalidException("Bạn cần đăng nhập để thực hiện thanh toán"));
        User user = userService.handleGetUserByUsername(email);
        if (user == null) {
            throw new IdInvalidException("Người dùng không tồn tại");
        }

        String orderId = String.valueOf(System.currentTimeMillis());
        long amount = 50000 * 100;
        String orderInfo = "Thanh toan goi VIP cho " + email;
        String ipAddr = request.getRemoteAddr();

        Map<String, String> vnpParams = new TreeMap<>();
        vnpParams.put("vnp_Version", vnpVersion);
        vnpParams.put("vnp_Command", vnpCommand);
        vnpParams.put("vnp_TmnCode", vnpTmnCode);
        vnpParams.put("vnp_Amount", String.valueOf(amount));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", orderId);
        vnpParams.put("vnp_OrderInfo", orderInfo);
        vnpParams.put("vnp_OrderType", "250000");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnpReturnUrl);
        vnpParams.put("vnp_IpAddr", ipAddr);
        vnpParams.put("vnp_CreateDate", String.format("%tY%tm%td%tH%tM%tS",
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(),
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()));
        vnpParams.put("vnp_ExpireDate", String.format("%tY%tm%td%tH%tM%tS",
                LocalDateTime.now().plusMinutes(15), LocalDateTime.now().plusMinutes(15),
                LocalDateTime.now().plusMinutes(15), LocalDateTime.now().plusMinutes(15),
                LocalDateTime.now().plusMinutes(15), LocalDateTime.now().plusMinutes(15)));

        StringBuilder hashData = new StringBuilder();
        for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
            hashData.append(entry.getKey()).append("=")
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)).append("&");
        }
        hashData.deleteCharAt(hashData.length() - 1);
        System.out.println("hashData: " + hashData.toString());

        String secureHash = hmacSHA512(vnpHashSecret, hashData.toString());
        System.out.println("vnp_SecureHash: " + secureHash);
        vnpParams.put("vnp_SecureHash", secureHash);

        StringBuilder paymentUrl = new StringBuilder(vnpPaymentUrl).append("?");
        for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
            paymentUrl.append(entry.getKey()).append("=")
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)).append("&");
        }
        paymentUrl.deleteCharAt(paymentUrl.length() - 1);

        RestResponse<PaymentUrlResponseDTO> response = new RestResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setMessage("Create Vnpay URL");
        response.setData(new PaymentUrlResponseDTO(paymentUrl.toString()));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/payment/vnpay/callback")
    @ApiMessage("Handle VNPay callback")
    public ResponseEntity<PaymentResponse> handleCallback(@RequestParam Map<String, String> params)
            throws IdInvalidException {
        String secureHash = params.get("vnp_SecureHash");
        params.remove("vnp_SecureHash");

        StringBuilder hashData = new StringBuilder();
        for (Map.Entry<String, String> entry : new TreeMap<>(params).entrySet()) {
            hashData.append(entry.getKey()).append("=")
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)).append("&");
        }
        hashData.deleteCharAt(hashData.length() - 1);
        String calculatedHash = hmacSHA512(vnpHashSecret, hashData.toString());

        PaymentResponse response = new PaymentResponse();
        if (!calculatedHash.equalsIgnoreCase(secureHash)) {
            response.setStatus("error");
            response.setMessage("Chữ ký không hợp lệ");
            return ResponseEntity.badRequest().body(response);
        }

        String responseCode = params.get("vnp_ResponseCode");
        String orderInfo = params.get("vnp_OrderInfo");
        String email = orderInfo.replace("Thanh toan goi VIP cho ", "");
        String orderId = params.get("vnp_TxnRef");
        long amount = Long.parseLong(params.get("vnp_Amount"));

        User user = userService.handleGetUserByUsername(email);
        if (user == null) {
            response.setStatus("error");
            response.setMessage("Người dùng không tồn tại");
            return ResponseEntity.badRequest().body(response);
        }

        PaymentHistory paymentHistory = new PaymentHistory();
        paymentHistory.setUser(user);
        paymentHistory.setAmount(amount);
        paymentHistory.setOrderId(orderId);
        paymentHistory.setResponseCode(responseCode);
        paymentHistory.setStatus(
                "00".equals(responseCode) ? PaymentHistory.PaymentStatus.SUCCESS : PaymentHistory.PaymentStatus.FAILED);
        paymentHistoryRepository.save(paymentHistory);

        if ("00".equals(responseCode)) {
            userService.activateVip(user);
            response.setStatus("success");
            response.setMessage("Thanh toán thành công, tài khoản VIP đã được kích hoạt");
        } else {
            response.setStatus("error");
            response.setMessage("Thanh toán thất bại: " + responseCode);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/payment/history")
    @ApiMessage("Get payment history")
    public ResponseEntity<List<PaymentHistoryDTO>> getPaymentHistory() throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(
                () -> new IdInvalidException("Bạn cần đăng nhập"));
        User user = userService.handleGetUserByUsername(email);
        if (user == null) {
            throw new IdInvalidException("Người dùng không tồn tại");
        }

        List<PaymentHistoryDTO> history = paymentHistoryRepository.findByUser(user)
                .stream()
                .map(ph -> new PaymentHistoryDTO(
                        ph.getId(),
                        ph.getUser().getEmail(),
                        ph.getUser().getId(),
                        ph.getAmount(),
                        ph.getOrderId(),
                        ph.getResponseCode(),
                        ph.getStatus(),
                        ph.getCreatedAt(),
                        ph.getUpdatedAt(),
                        ph.getUpdatedBy()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(history);
    }

    @GetMapping("/payment/allhistory")
    @ApiMessage("Get all payment history with pagination")
    public ResponseEntity<ResultPaginationDTO> getAllPaymentHistory(
            @Filter Specification<PaymentHistory> spec,
            Pageable pageable) throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(
                () -> new IdInvalidException("Bạn cần đăng nhập"));
        User user = userService.handleGetUserByUsername(email);
        if (user == null) {
            throw new IdInvalidException("Người dùng không tồn tại");
        }

        Page<PaymentHistory> paymentPage = paymentHistoryRepository.findAll(spec, pageable);
        List<PaymentHistoryDTO> history = paymentPage.getContent()
                .stream()
                .map(ph -> new PaymentHistoryDTO(
                        ph.getId(),
                        ph.getUser().getEmail(),
                        ph.getUser().getId(),
                        ph.getAmount(),
                        ph.getOrderId(),
                        ph.getResponseCode(),
                        ph.getStatus(),
                        ph.getCreatedAt(),
                        ph.getUpdatedAt(),
                        ph.getUpdatedBy()))
                .collect(Collectors.toList());

        ResultPaginationDTO result = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(paymentPage.getTotalPages());
        meta.setTotal(paymentPage.getTotalElements());
        result.setMeta(meta);
        result.setResult(history);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/payment/allhistory/{id}")
    @ApiMessage("Get payment history by ID")
    public ResponseEntity<PaymentHistoryDTO> getPaymentHistoryById(@PathVariable("id") Long id)
            throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(
                () -> new IdInvalidException("Bạn cần đăng nhập"));
        User user = userService.handleGetUserByUsername(email);
        if (user == null) {
            throw new IdInvalidException("Người dùng không tồn tại");
        }

        PaymentHistory paymentHistory = paymentHistoryRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Payment history với ID " + id + " không tồn tại"));
        PaymentHistoryDTO dto = new PaymentHistoryDTO(
                paymentHistory.getId(),
                paymentHistory.getUser().getEmail(),
                paymentHistory.getUser().getId(),
                paymentHistory.getAmount(),
                paymentHistory.getOrderId(),
                paymentHistory.getResponseCode(),
                paymentHistory.getStatus(),
                paymentHistory.getCreatedAt(),
                paymentHistory.getUpdatedAt(),
                paymentHistory.getUpdatedBy());

        return ResponseEntity.ok(dto);
    }

    @PutMapping("/payment/allhistory")
    @ApiMessage("Update payment history status")
    public ResponseEntity<PaymentHistoryDTO> updatePaymentHistoryStatus(
            @RequestBody UpdatePaymentStatusDTO request) throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(
                () -> new IdInvalidException("Bạn cần đăng nhập"));
        User user = userService.handleGetUserByUsername(email);
        if (user == null) {
            throw new IdInvalidException("Người dùng không tồn tại");
        }

        PaymentHistory paymentHistory = paymentHistoryRepository.findById(request.getId())
                .orElseThrow(
                        () -> new IdInvalidException("Payment history với ID " + request.getId() + " không tồn tại"));

        if (!request.getStatus().equals(PaymentHistory.PaymentStatus.SUCCESS.toString()) &&
                !request.getStatus().equals(PaymentHistory.PaymentStatus.FAILED.toString())) {
            throw new IdInvalidException("Trạng thái chỉ được là SUCCESS hoặc FAILED");
        }

        paymentHistory.setStatus(PaymentHistory.PaymentStatus.valueOf(request.getStatus()));
        paymentHistoryRepository.save(paymentHistory);

        PaymentHistoryDTO dto = new PaymentHistoryDTO(
                paymentHistory.getId(),
                paymentHistory.getUser().getEmail(),
                paymentHistory.getUser().getId(),
                paymentHistory.getAmount(),
                paymentHistory.getOrderId(),
                paymentHistory.getResponseCode(),
                paymentHistory.getStatus(),
                paymentHistory.getCreatedAt(),
                paymentHistory.getUpdatedAt(),
                paymentHistory.getUpdatedBy());

        return ResponseEntity.ok(dto);
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo chữ ký HMAC-SHA512", e);
        }
    }

    @GetMapping("/payment/export/excel")
    @ApiMessage("Export payment history to Excel")
    public ResponseEntity<Resource> exportPaymentExcel(@Filter Specification<PaymentHistory> spec)
            throws IdInvalidException, IOException {

        // 1. Kiểm tra đăng nhập (giữ nguyên logic bảo mật của bạn)
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(
                () -> new IdInvalidException("Bạn cần đăng nhập"));
        User user = userService.handleGetUserByUsername(email);
        if (user == null)
            throw new IdInvalidException("Người dùng không tồn tại");

        // 2. Lấy dữ liệu: Dùng spec để lọc nhưng KHÔNG phân trang (lấy tất cả kết quả
        // lọc)
        List<PaymentHistory> histories = paymentHistoryRepository.findAll(spec);

        // 3. Gọi service tạo file
        ByteArrayInputStream in = paymentExportService.exportToExcel(histories);

        // 4. Trả về file
        InputStreamResource file = new InputStreamResource(in);
        String filename = "Payment_Report_" + System.currentTimeMillis() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }

    // --- API XUẤT WORD ---
    @GetMapping("/payment/export/monthly-report")
    @ApiMessage("Export monthly revenue report via Template")
    public ResponseEntity<Resource> exportMonthlyReport(
            @RequestParam("month") int month,
            @RequestParam("year") int year) throws IOException, IdInvalidException {

        // 1. Lấy thông tin người request (để điền vào mục Người lập báo cáo)
        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(
                () -> new IdInvalidException("Bạn cần đăng nhập"));

        // 2. Gọi Service
        ByteArrayInputStream in = paymentExportService.exportReportByMonth(month, year, email);

        // 3. Trả về file
        InputStreamResource file = new InputStreamResource(in);
        String filename = "Bao_Cao_Thang_" + month + "_" + year + ".docx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType
                        .parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(file);
    }

    @GetMapping("/payment/export/yearly-report")
    @ApiMessage("Export yearly revenue report")
    public ResponseEntity<Resource> exportYearlyReport(
            @RequestParam("year") int year) throws IOException, IdInvalidException {

        String email = SecurityUtil.getCurrentUserLogin().orElseThrow(
                () -> new IdInvalidException("Bạn cần đăng nhập"));

        // Gọi hàm exportByYear
        ByteArrayInputStream in = paymentExportService.exportReportByYear(year, email);

        InputStreamResource file = new InputStreamResource(in);
        String filename = "Bao_Cao_Nam_" + year + ".docx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType
                        .parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(file);
    }

    static class PaymentResponse {
        private String status;
        private String message;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    static class UpdatePaymentStatusDTO {
        private Long id;
        private String status;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}