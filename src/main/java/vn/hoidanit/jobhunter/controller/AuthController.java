package vn.hoidanit.jobhunter.controller;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import vn.hoidanit.jobhunter.domain.entity.User;
import vn.hoidanit.jobhunter.domain.entity.UserSession;
import vn.hoidanit.jobhunter.domain.request.ReqChangePasswordDTO;
import vn.hoidanit.jobhunter.domain.request.ReqGoogleLoginDTO;
import vn.hoidanit.jobhunter.domain.request.ReqDeleteSessionsDTO;

import vn.hoidanit.jobhunter.domain.request.ReqLoginDTO;
import vn.hoidanit.jobhunter.domain.request.ReqSendOtpDTO;
import vn.hoidanit.jobhunter.domain.request.ReqUserRegisterDTO;
import vn.hoidanit.jobhunter.domain.request.ReqVerifyOtpChangePasswordDTO;
import vn.hoidanit.jobhunter.domain.response.ResChangePasswordDTO;
import vn.hoidanit.jobhunter.domain.response.ResCreateUserDTO;
import vn.hoidanit.jobhunter.domain.response.ResLoginDTO;
import vn.hoidanit.jobhunter.domain.response.ResSessionDTO;
import vn.hoidanit.jobhunter.repository.UserSessionRepository;
import vn.hoidanit.jobhunter.service.OtpService;
import vn.hoidanit.jobhunter.service.RedisTokenBlacklistService;
import vn.hoidanit.jobhunter.service.UserService;
import vn.hoidanit.jobhunter.service.UserSessionService;
import vn.hoidanit.jobhunter.util.SecurityUtil;
import vn.hoidanit.jobhunter.util.annotation.ApiMessage;
import vn.hoidanit.jobhunter.util.error.IdInvalidException;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final SecurityUtil securityUtil;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final RedisTokenBlacklistService blacklistService; // Thêm service
    private final JwtDecoder jwtDecoder; // Thêm decoder để đọc token khi logout
    private final UserSessionService userSessionService;
    private final UserSessionRepository userSessionRepository;

    @Value("${hoidanit.jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    public AuthController(AuthenticationManagerBuilder authenticationManagerBuilder,
            SecurityUtil securityUtil, UserService userService, PasswordEncoder passwordEncoder,
            OtpService otpService, RedisTokenBlacklistService blacklistService, JwtDecoder jwtDecoder,
            UserSessionService userSessionService, UserSessionRepository userSessionRepository) { // Thêm
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.securityUtil = securityUtil;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.blacklistService = blacklistService; // Thêm
        this.jwtDecoder = jwtDecoder; // Thêm
        this.userSessionService = userSessionService;
        this.userSessionRepository = userSessionRepository;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<ResLoginDTO> login(@Valid @RequestBody ReqLoginDTO loginDto, HttpServletRequest request) {
        // Nạp input gồm username/password vào Security
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                loginDto.getUsername(), loginDto.getPassword());

        // xác thực người dùng => cần viết hàm loadUserByUsername
        Authentication authentication = authenticationManagerBuilder.getObject()
                .authenticate(authenticationToken);

        // set thông tin người dùng đăng nhập vào context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        ResLoginDTO res = new ResLoginDTO();
        User currentUserDB = this.userService.handleGetUserByUsername(loginDto.getUsername());
        if (currentUserDB != null) {
            ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin(
                    currentUserDB.getId(),
                    currentUserDB.getEmail(),
                    currentUserDB.getName(),
                    currentUserDB.getGender(),
                    currentUserDB.getAddress(),
                    currentUserDB.getAge(),
                    currentUserDB.getAvatar(),
                    currentUserDB.isPublic(),
                    currentUserDB.getRole(),
                    currentUserDB.isVip(),
                    currentUserDB.getVipExpiryDate(),

                    currentUserDB.getCompany() != null
                            ? new ResLoginDTO.UserLogin.CompanyUser(
                                    currentUserDB.getCompany().getId(),
                                    currentUserDB.getCompany().getName(),
                                    currentUserDB.getCompany().getDescription(),
                                    currentUserDB.getCompany().getAddress(),
                                    currentUserDB.getCompany().getLogo(),
                                    currentUserDB.getCompany().getField(),
                                    currentUserDB.getCompany().getWebsite(),
                                    currentUserDB.getCompany().getScale(),
                                    currentUserDB.getCompany().getCountry(),
                                    currentUserDB.getCompany().getFoundingYear(),
                                    currentUserDB.getCompany().getLocation())

                            : null);
            res.setUser(userLogin);
        }

        // create access token
        String access_token = this.securityUtil.createAccessToken(authentication.getName(), res);
        res.setAccessToken(access_token);

        // create refresh token
        String refresh_token = this.securityUtil.createRefreshToken(loginDto.getUsername(), res);

        // SỬA LOGIC LƯU TOKEN:
        // 1. Lấy JTI và Expiry từ refresh token
        Jwt decodedRefresh = this.securityUtil.checkValidRefreshToken(refresh_token);
        String jti = decodedRefresh.getClaimAsString("jti");
        Instant expiresAt = decodedRefresh.getExpiresAt();

        // 2. Không lưu vào User, mà tạo session mới
        // this.userService.updateUserToken(refresh_token, loginDto.getUsername()); //
        // <<< XÓA DÒNG NÀY
        this.userService.createNewSession(currentUserDB, jti, expiresAt);

        // set cookies
        ResponseCookie resCookies = ResponseCookie
                .from("refresh_token", refresh_token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(refreshTokenExpiration)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, resCookies.toString())
                .body(res);
    }

    @PostMapping("/auth/google")
    @ApiMessage("Đăng nhập bằng Google")
    public ResponseEntity<ResLoginDTO> googleLogin(@Valid @RequestBody ReqGoogleLoginDTO googleLoginDTO)
            throws IdInvalidException {
        try {
            // Xác minh Google ID token
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(),
                    new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(googleLoginDTO.getCredential());
            if (idToken == null) {
                throw new IdInvalidException("Google ID token không hợp lệ");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            // Kiểm tra user trong database
            User user = userService.handleGetUserByUsername(email);
            if (user == null) {
                // Tạo user mới với password null
                user = new User();
                user.setEmail(email);
                user.setName(name);
                user.setPassword(null); // Không đặt mật khẩu
                user = userService.handleCreateUser(user);
            }

            // Tạo authentication cho SecurityContext
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    email, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            // Tạo response
            ResLoginDTO res = new ResLoginDTO();
            ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin(
                    user.getId(),
                    user.getEmail(),
                    user.getName(),
                    user.getGender(),
                    user.getAddress(),
                    user.getAge(),
                    user.getAvatar(),
                    user.isPublic(),
                    user.getRole(),
                    user.isVip(),
                    user.getVipExpiryDate(),

                    user.getCompany() != null
                            ? new ResLoginDTO.UserLogin.CompanyUser(
                                    user.getCompany().getId(),
                                    user.getCompany().getName(),
                                    user.getCompany().getDescription(),
                                    user.getCompany().getAddress(),
                                    user.getCompany().getLogo(),
                                    user.getCompany().getField(),
                                    user.getCompany().getWebsite(),
                                    user.getCompany().getScale(),
                                    user.getCompany().getCountry(),
                                    user.getCompany().getFoundingYear(),
                                    user.getCompany().getLocation())
                            : null);
            res.setUser(userLogin);

            // Tạo access token
            String accessToken = securityUtil.createAccessToken(email, res);
            res.setAccessToken(accessToken);

            // Tạo refresh token
            String refreshToken = securityUtil.createRefreshToken(email, res);
            Jwt decodedRefresh = this.securityUtil.checkValidRefreshToken(refreshToken);
            String jti = decodedRefresh.getClaimAsString("jti");
            Instant expiresAt = decodedRefresh.getExpiresAt();
            this.userService.createNewSession(user, jti, expiresAt);
            // Set cookie cho refresh token
            ResponseCookie resCookies = ResponseCookie
                    .from("refresh_token", refreshToken)
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(refreshTokenExpiration)
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, resCookies.toString())
                    .body(res);
        } catch (Exception e) {
            throw new IdInvalidException("Lỗi xác minh Google token: " + e.getMessage());
        }
    }

    @GetMapping("/auth/account")
    @ApiMessage("fetch account")
    public ResponseEntity<ResLoginDTO.UserGetAccount> getAccount() {
        String email = SecurityUtil.getCurrentUserLogin().isPresent()
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";

        User currentUserDB = this.userService.handleGetUserByUsername(email);
        ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin();
        ResLoginDTO.UserGetAccount userGetAccount = new ResLoginDTO.UserGetAccount();

        if (currentUserDB != null) {
            userLogin.setId(currentUserDB.getId());
            userLogin.setEmail(currentUserDB.getEmail());
            userLogin.setName(currentUserDB.getName());
            userLogin.setGender(currentUserDB.getGender());
            userLogin.setAddress(currentUserDB.getAddress());
            userLogin.setAge(currentUserDB.getAge());
            userLogin.setAvatar(currentUserDB.getAvatar());
            userLogin.setPublic(currentUserDB.isPublic());
            userLogin.setRole(currentUserDB.getRole());
            userLogin.setVip(currentUserDB.isVip());
            userLogin.setVipExpiryDate(currentUserDB.getVipExpiryDate());

            userLogin.setCompany(currentUserDB.getCompany() != null
                    ? new ResLoginDTO.UserLogin.CompanyUser(
                            currentUserDB.getCompany().getId(),
                            currentUserDB.getCompany().getName(),
                            currentUserDB.getCompany().getDescription(),
                            currentUserDB.getCompany().getAddress(),
                            currentUserDB.getCompany().getLogo(),
                            currentUserDB.getCompany().getField(),
                            currentUserDB.getCompany().getWebsite(),
                            currentUserDB.getCompany().getScale(),
                            currentUserDB.getCompany().getCountry(),
                            currentUserDB.getCompany().getFoundingYear(),
                            currentUserDB.getCompany().getLocation())
                    : null);
            userGetAccount.setUser(userLogin);
        }

        return ResponseEntity.ok().body(userGetAccount);
    }

    // ========== SỬA PHƯƠNG THỨC NÀY ==========
    @GetMapping("/auth/refresh")
    @ApiMessage("Get User by refresh token")
    public ResponseEntity<ResLoginDTO> getRefreshToken(
            @CookieValue(name = "refresh_token", defaultValue = "abc") String refresh_token) throws IdInvalidException {

        if (refresh_token.equals("abc")) {
            throw new IdInvalidException("Bạn không có refresh token ở cookie");
        }

        // Bước 1: Decode
        Jwt decodedToken = this.securityUtil.checkValidRefreshToken(refresh_token);
        String email = decodedToken.getSubject();
        String jti = decodedToken.getClaimAsString("jti");

        // Bước 2: Kiểm tra JTI Blacklist (bắt các token đã bị refresh/logout)
        if (blacklistService.isTokenBlacklisted(jti)) {
            throw new IdInvalidException("Refresh Token đã bị vô hiệu hóa. Vui lòng đăng nhập lại.");
        }

        // Bước 3: Kiểm tra token trong DB (bắt lỗi của bạn)
        UserSession session = this.userService.findSessionByJti(jti)
                .orElseThrow(() -> new IdInvalidException("Refresh Token không hợp lệ (Session không tồn tại)"));

        // Bước 4: Tạo token mới
        ResLoginDTO res = new ResLoginDTO();
        User currentUserDB = this.userService.handleGetUserByUsername(email); // Lấy user mới nhất
        if (currentUserDB != null) {
            ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin(
                    currentUserDB.getId(),
                    currentUserDB.getEmail(),
                    currentUserDB.getName(),
                    currentUserDB.getGender(),
                    currentUserDB.getAddress(),
                    currentUserDB.getAge(),
                    currentUserDB.getAvatar(),
                    currentUserDB.isPublic(),
                    currentUserDB.getRole(),
                    currentUserDB.isVip(),
                    currentUserDB.getVipExpiryDate(),
                    currentUserDB.getCompany() != null
                            ? new ResLoginDTO.UserLogin.CompanyUser(
                                    currentUserDB.getCompany().getId(),
                                    currentUserDB.getCompany().getName(),
                                    currentUserDB.getCompany().getDescription(),
                                    currentUserDB.getCompany().getAddress(),
                                    currentUserDB.getCompany().getLogo(),
                                    currentUserDB.getCompany().getField(),
                                    currentUserDB.getCompany().getWebsite(),
                                    currentUserDB.getCompany().getScale(),
                                    currentUserDB.getCompany().getCountry(),
                                    currentUserDB.getCompany().getFoundingYear(),
                                    currentUserDB.getCompany().getLocation())
                            : null);
            res.setUser(userLogin);
        }

        String access_token = this.securityUtil.createAccessToken(email, res);
        res.setAccessToken(access_token);

        String new_refresh_token = this.securityUtil.createRefreshToken(email, res);

        // Bước 5: Cập nhật DB
        // this.userService.updateUserToken(new_refresh_token, email);
        this.userService.deleteSessionByJti(jti);
        Jwt newDecodedRefresh = this.securityUtil.checkValidRefreshToken(new_refresh_token);
        String newJti = newDecodedRefresh.getClaimAsString("jti");
        Instant newExpiresAt = newDecodedRefresh.getExpiresAt();
        this.userService.createNewSession(session.getUser(), newJti, newExpiresAt); // Dùng user từ session cũ

        // Bước 6: ĐƯA TOKEN CŨ VÀO BLACKLIST
        Instant oldTokenExpiry = decodedToken.getExpiresAt();
        blacklistService.blacklistToken(jti, oldTokenExpiry);

        // Bước 7: Set cookie
        ResponseCookie resCookies = ResponseCookie
                .from("refresh_token", new_refresh_token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(refreshTokenExpiration)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, resCookies.toString())
                .body(res);
    }
    // ========== KẾT THÚC SỬA ==========

    // ========== SỬA LẠI HOÀN TOÀN PHƯƠNG THỨC NÀY ==========
    @PostMapping("/auth/logout")
    @ApiMessage("Logout User")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token", defaultValue = "abc") String refresh_token)
            throws IdInvalidException {

        String email = SecurityUtil.getCurrentUserLogin().orElse(null);
        if (email == null) {
            throw new IdInvalidException("Access Token không hợp lệ");
        }

        // 1. Blacklist Access Token (lấy từ header)
        SecurityUtil.getCurrentUserJWT().ifPresent(token -> {
            try {
                Jwt decodedAccess = this.jwtDecoder.decode(token);
                String jti = decodedAccess.getClaimAsString("jti");
                Instant expiry = decodedAccess.getExpiresAt();
                blacklistService.blacklistToken(jti, expiry);
            } catch (Exception e) {
                // Bỏ qua
            }
        });

        // 2. Blacklist Refresh Token (lấy từ cookie)
        if (!refresh_token.equals("abc")) {
            try {
                Jwt decodedRefresh = this.securityUtil.checkValidRefreshToken(refresh_token);
                String jti = decodedRefresh.getClaimAsString("jti");
                Instant expiry = decodedRefresh.getExpiresAt();

                // Thêm vào blacklist
                blacklistService.blacklistToken(jti, expiry);

                // Xóa khỏi DB session
                this.userService.deleteSessionByJti(jti);
            } catch (Exception e) {
                // Bỏ qua
            }
        }

        // 3. Xóa token trong DB
        // this.userService.updateUserToken(null, email);

        // 4. Xóa cookie
        ResponseCookie deleteSpringCookie = ResponseCookie
                .from("refresh_token", null)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteSpringCookie.toString())
                .body(null);
    }

    // ========== KẾT THÚC SỬA ==========
    /**
     * Endpoint mới: Gửi OTP để xác thực email đăng ký.
     */
    @PostMapping("/auth/register/send-otp")
    @ApiMessage("Gửi mã OTP để đăng ký tài khoản")
    public ResponseEntity<Void> sendRegistrationOtp(@Valid @RequestBody ReqSendOtpDTO sendOtpDTO)
            throws IdInvalidException {
        // Kiểm tra xem email đã được đăng ký chưa
        if (userService.isEmailExist(sendOtpDTO.getEmail())) {
            throw new IdInvalidException(
                    "Email " + sendOtpDTO.getEmail() + " đã tồn tại, vui lòng sử dụng email khác.");
        }

        // Tạo, lưu và gửi OTP
        String otpCode = otpService.generateOtp();
        otpService.saveOtp(sendOtpDTO.getEmail(), otpCode);
        // Gửi email với tiêu đề rõ ràng
        otpService.sendOtpEmail(sendOtpDTO.getEmail(), otpCode, "Mã OTP Đăng Ký Tài Khoản JobHunter");

        return ResponseEntity.ok().build();
    }

    /**
     * Cập nhật API /register để xác thực OTP và không nhận 'role'
     */
    @PostMapping("/auth/register")
    @ApiMessage("Register a new user")
    public ResponseEntity<ResCreateUserDTO> register(@Valid @RequestBody ReqUserRegisterDTO registerDTO)
            throws IdInvalidException {
        // 1. Xác thực OTP có hợp lệ không
        otpService.validateOtp(registerDTO.getEmail(), registerDTO.getOtpCode());

        // 2. Kiểm tra lại sự tồn tại của email một lần nữa (đề phòng race condition)
        if (this.userService.isEmailExist(registerDTO.getEmail())) {
            throw new IdInvalidException(
                    "Email " + registerDTO.getEmail() + " đã tồn tại, vui lòng sử dụng email khác.");
        }

        // 3. Chuyển đổi DTO thành User entity
        // Vì ReqUserRegisterDTO không có trường 'role', user.getRole() sẽ là null
        User user = new User();
        user.setName(registerDTO.getName());
        user.setEmail(registerDTO.getEmail());
        user.setPassword(this.passwordEncoder.encode(registerDTO.getPassword()));
        user.setAge(registerDTO.getAge());
        user.setGender(registerDTO.getGender());
        user.setAddress(registerDTO.getAddress());

        // 4. Gọi service để tạo user.
        // Phương thức handleCreateUser hiện tại sẽ bỏ qua việc set role nếu nó là null
        User newUser = this.userService.handleCreateUser(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(this.userService.convertToResCreateUserDTO(newUser));
    }

    @PostMapping("/auth/change-password")
    @ApiMessage("Đổi mật khẩu")
    public ResponseEntity<ResChangePasswordDTO> changePassword( // 1. Đổi kiểu trả về
            @Valid @RequestBody ReqChangePasswordDTO changePasswordDTO,
            @CookieValue(name = "refresh_token", defaultValue = "abc") String current_refresh_token) // 2. Lấy token
                                                                                                     // hiện tại
            throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy user"));
        User user = userService.handleGetUserByUsername(email);
        if (user == null) {
            throw new IdInvalidException("User không tồn tại");
        }

        if (user.getPassword() != null) {
            if (changePasswordDTO.getOldPassword() == null
                    || !passwordEncoder.matches(changePasswordDTO.getOldPassword(), user.getPassword())) {
                throw new IdInvalidException("Mật khẩu cũ không đúng...");
            }
        }

        // 3. SỬA BUG & THÊM TÍNH NĂNG:
        // Gọi service đã tạo ở Bước 3 để lưu pass + cập nhật timestamp
        User updatedUser = userService.saveUserWithNewPassword(user, changePasswordDTO.getNewPassword());

        // 4. TẠO TOKEN MỚI (để giữ user đăng nhập)
        ResLoginDTO newLoginDto = new ResLoginDTO();
        ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin(
                updatedUser.getId(), updatedUser.getEmail(), updatedUser.getName(),
                updatedUser.getGender(), updatedUser.getAddress(), updatedUser.getAge(),
                updatedUser.getAvatar(), updatedUser.isPublic(), updatedUser.getRole(),
                updatedUser.isVip(), updatedUser.getVipExpiryDate(),
                updatedUser.getCompany() != null
                        ? new ResLoginDTO.UserLogin.CompanyUser(
                                updatedUser.getCompany().getId(), updatedUser.getCompany().getName(),
                                updatedUser.getCompany().getDescription(), updatedUser.getCompany().getAddress(),
                                updatedUser.getCompany().getLogo(), updatedUser.getCompany().getField(),
                                updatedUser.getCompany().getWebsite(), updatedUser.getCompany().getScale(),
                                updatedUser.getCompany().getCountry(), updatedUser.getCompany().getFoundingYear(),
                                updatedUser.getCompany().getLocation())
                        : null);
        newLoginDto.setUser(userLogin);

        String access_token = this.securityUtil.createAccessToken(email, newLoginDto);
        newLoginDto.setAccessToken(access_token);
        String new_refresh_token = this.securityUtil.createRefreshToken(email, newLoginDto);

        // 5. CẬP NHẬT SESSION TRONG DB
        // Xóa session cũ (của refresh token hiện tại)
        if (!current_refresh_token.equals("abc")) {
            try {
                Jwt decodedOld = this.securityUtil.checkValidRefreshToken(current_refresh_token);
                String oldJti = decodedOld.getClaimAsString("jti");
                this.userService.deleteSessionByJti(oldJti);
                this.blacklistService.blacklistToken(oldJti, decodedOld.getExpiresAt());
            } catch (Exception e) {
                /* Bỏ qua */ }
        }

        // Tạo session mới (cho refresh token mới)
        Jwt decodedNew = this.securityUtil.checkValidRefreshToken(new_refresh_token);
        String newJti = decodedNew.getClaimAsString("jti");
        Instant newExpiresAt = decodedNew.getExpiresAt();
        this.userService.createNewSession(updatedUser, newJti, newExpiresAt);

        // 6. LẤY DANH SÁCH CÁC SESSION CÒN LẠI
        // (Timestamp đã vô hiệu hóa access token của chúng, nhưng refresh token vẫn
        // còn)
        List<UserSession> allSessions = this.userService.getSessionsForUser(updatedUser.getId());

        // 7. TẠO RESPONSE BODY
        ResChangePasswordDTO responseBody = new ResChangePasswordDTO(newLoginDto, allSessions, newJti);

        // 8. TẠO COOKIE MỚI
        ResponseCookie resCookies = ResponseCookie
                .from("refresh_token", new_refresh_token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(refreshTokenExpiration)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, resCookies.toString())
                .body(responseBody);
    }

    @PostMapping("/auth/send-otp")
    @ApiMessage("Gửi mã OTP để đổi mật khẩu")
    public ResponseEntity<Void> sendOtp(@Valid @RequestBody ReqSendOtpDTO sendOtpDTO) throws IdInvalidException {
        String email = sendOtpDTO.getEmail();
        User user = userService.handleGetUserByUsername(email);
        if (user == null) {
            throw new IdInvalidException("Email không tồn tại");
        }

        String otpCode = otpService.generateOtp();
        otpService.saveOtp(email, otpCode);
        otpService.sendOtpEmail(email, otpCode);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/auth/verify-otp-change-password")
    @ApiMessage("Xác minh OTP và đổi mật khẩu")
    public ResponseEntity<ResLoginDTO> verifyOtpAndChangePassword( // 1. Đổi kiểu trả về
            @Valid @RequestBody ReqVerifyOtpChangePasswordDTO verifyOtpDTO) throws IdInvalidException {
        otpService.validateOtp(verifyOtpDTO.getEmail(), verifyOtpDTO.getOtpCode());
        User user = userService.handleGetUserByUsername(verifyOtpDTO.getEmail());
        if (user == null) {
            throw new IdInvalidException("User không tồn tại");
        }

        // 2. Cập nhật password VÀ timestamp
        User updatedUser = userService.saveUserWithNewPassword(user, verifyOtpDTO.getNewPassword());

        // 3. VÔ HIỆU HÓA TẤT CẢ SESSION CŨ
        List<UserSession> allSessions = this.userService.getSessionsForUser(updatedUser.getId());
        if (allSessions != null && !allSessions.isEmpty()) {
            this.userSessionRepository.deleteAll(allSessions);
        }

        // 4. TẠO TOKEN MỚI (để đăng nhập ngay)
        ResLoginDTO res = new ResLoginDTO();
        ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin(
                updatedUser.getId(), updatedUser.getEmail(), updatedUser.getName(),
                updatedUser.getGender(), updatedUser.getAddress(), updatedUser.getAge(),
                updatedUser.getAvatar(), updatedUser.isPublic(), updatedUser.getRole(),
                updatedUser.isVip(), updatedUser.getVipExpiryDate(),
                updatedUser.getCompany() != null
                        ? new ResLoginDTO.UserLogin.CompanyUser(
                                updatedUser.getCompany().getId(), updatedUser.getCompany().getName(),
                                updatedUser.getCompany().getDescription(), updatedUser.getCompany().getAddress(),
                                updatedUser.getCompany().getLogo(), updatedUser.getCompany().getField(),
                                updatedUser.getCompany().getWebsite(), updatedUser.getCompany().getScale(),
                                updatedUser.getCompany().getCountry(), updatedUser.getCompany().getFoundingYear(),
                                updatedUser.getCompany().getLocation())
                        : null);
        res.setUser(userLogin);
        String access_token = this.securityUtil.createAccessToken(updatedUser.getEmail(), res);
        res.setAccessToken(access_token);
        String refresh_token = this.securityUtil.createRefreshToken(updatedUser.getEmail(), res);

        // 5. TẠO SESSION MỚI
        Jwt decodedNew = this.securityUtil.checkValidRefreshToken(refresh_token);
        String newJti = decodedNew.getClaimAsString("jti");
        Instant newExpiresAt = decodedNew.getExpiresAt();
        this.userService.createNewSession(updatedUser, newJti, newExpiresAt);

        // 6. TẠO COOKIE
        ResponseCookie resCookies = ResponseCookie
                .from("refresh_token", refresh_token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(refreshTokenExpiration)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, resCookies.toString())
                .body(res);
    }

    @GetMapping("/auth/sessions")
    @ApiMessage("Lấy danh sách các thiết bị đang đăng nhập")
    public ResponseEntity<List<ResSessionDTO>> getActiveSessions(
            @CookieValue(name = "refresh_token", defaultValue = "abc") String current_refresh_token)
            throws IdInvalidException {

        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy user"));
        User user = userService.handleGetUserByUsername(email);
        if (user == null) {
            throw new IdInvalidException("User không tồn tại");
        }

        // Lấy JTI của session hiện tại
        String currentJti = null;
        if (!current_refresh_token.equals("abc")) {
            try {
                currentJti = this.securityUtil.checkValidRefreshToken(current_refresh_token).getClaimAsString("jti");
            } catch (Exception e) {
                /* Bỏ qua */ }
        }

        List<UserSession> sessions = this.userService.getSessionsForUser(user.getId());

        final String finalCurrentJti = currentJti;
        List<ResSessionDTO> dtos = sessions.stream()
                .map(s -> new ResSessionDTO(s, finalCurrentJti))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // --- API MỚI 2: Đăng xuất một session cụ thể (theo ID session) ---
    @DeleteMapping("/auth/sessions/{id}")
    @ApiMessage("Đăng xuất một thiết bị cụ thể")
    public ResponseEntity<Void> logoutSession(
            @PathVariable("id") long sessionId,
            @CookieValue(name = "refresh_token", defaultValue = "abc") String current_refresh_token) // Lấy cookie
            throws IdInvalidException {

        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy user"));
        User user = userService.handleGetUserByUsername(email);
        if (user == null) {
            throw new IdInvalidException("User không tồn tại");
        }

        // Lấy JTI hiện tại để đảm bảo không tự xóa mình
        if (current_refresh_token.equals("abc")) {
            throw new IdInvalidException("Không tìm thấy refresh token hiện tại");
        }
        String currentJti = this.securityUtil.checkValidRefreshToken(current_refresh_token).getClaimAsString("jti");
        UserSession currentSession = this.userService.findSessionByJti(currentJti)
                .orElseThrow(() -> new IdInvalidException("Session hiện tại không hợp lệ"));

        // Ngăn user xóa chính mình
        if (currentSession.getId() == sessionId) {
            throw new IdInvalidException("Bạn không thể xóa session hiện tại. Vui lòng dùng /auth/logout.");
        }

        // 1. Xóa session (Vô hiệu hóa Refresh Token của nó)
        this.userService.deleteSessionById(sessionId, user.getId());

        // 2. Cập nhật timestamp (Vô hiệu hóa Access Token của nó)
        user.setLastSecurityUpdateAt(Instant.now());
        this.userService.saveUser(user); // 'saveUser' sẽ dọn dẹp cache

        // 3. Trả về 200 OK.
        // Lần gọi API tiếp theo của thiết bị hiện tại sẽ fail 401,
        // và FE interceptor sẽ tự động refresh.
        return ResponseEntity.ok().build();
    }

    // ========== API XÓA NHIỀU SESSION ==========
    @DeleteMapping("/auth/sessions")
    @ApiMessage("Đăng xuất nhiều thiết bị cụ thể")
    public ResponseEntity<Void> logoutSessions(
            @Valid @RequestBody ReqDeleteSessionsDTO deleteDTO,
            @CookieValue(name = "refresh_token", defaultValue = "abc") String current_refresh_token) // Lấy cookie
            throws IdInvalidException {

        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy user"));
        User user = userService.handleGetUserByUsername(email);
        if (user == null) {
            throw new IdInvalidException("User không tồn tại");
        }

        // Lấy session HIỆN TẠI để tránh tự xóa
        if (current_refresh_token.equals("abc")) {
            throw new IdInvalidException("Không tìm thấy refresh token hiện tại");
        }
        String currentJti = this.securityUtil.checkValidRefreshToken(current_refresh_token).getClaimAsString("jti");
        UserSession currentSession = this.userService.findSessionByJti(currentJti)
                .orElseThrow(() -> new IdInvalidException("Session hiện tại không hợp lệ"));

        // Lọc danh sách: Xóa ID của session hiện tại khỏi danh sách (nếu có)
        List<Long> idsToDelete = deleteDTO.getIds();
        if (idsToDelete != null && !idsToDelete.isEmpty()) {
            idsToDelete.remove(currentSession.getId());
        }

        // 1. Xóa các session mục tiêu (Vô hiệu hóa REFRESH tokens)
        this.userService.deleteSessionsByIds(idsToDelete, user.getId());

        // 2. Cập nhật timestamp (Vô hiệu hóa ACCESS tokens)
        user.setLastSecurityUpdateAt(Instant.now());
        this.userService.saveUser(user); // 'saveUser' sẽ dọn dẹp cache

        // 3. Trả về 200 OK.
        // FE sẽ tự refresh token ở lần gọi API 401 tiếp theo.
        return ResponseEntity.ok().build();
    }
}