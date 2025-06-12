package vn.hoidanit.jobhunter.controller;

import java.util.Collections;

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
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import jakarta.validation.Valid;
import vn.hoidanit.jobhunter.domain.entity.User;
import vn.hoidanit.jobhunter.domain.request.ReqChangePasswordDTO;
import vn.hoidanit.jobhunter.domain.request.ReqGoogleLoginDTO;
import vn.hoidanit.jobhunter.domain.request.ReqLoginDTO;
import vn.hoidanit.jobhunter.domain.request.ReqSendOtpDTO;
import vn.hoidanit.jobhunter.domain.request.ReqVerifyOtpChangePasswordDTO;
import vn.hoidanit.jobhunter.domain.response.ResCreateUserDTO;
import vn.hoidanit.jobhunter.domain.response.ResLoginDTO;
import vn.hoidanit.jobhunter.service.OtpService;
import vn.hoidanit.jobhunter.service.UserService;
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

    @Value("${hoidanit.jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    public AuthController(AuthenticationManagerBuilder authenticationManagerBuilder,
            SecurityUtil securityUtil, UserService userService, PasswordEncoder passwordEncoder,
            OtpService otpService) {
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.securityUtil = securityUtil;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<ResLoginDTO> login(@Valid @RequestBody ReqLoginDTO loginDto) {
        // Nạp input gồm username/password vào Security
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                loginDto.getUsername(), loginDto.getPassword());

        // xác thực người dùng => cần viết hàm loadUserByUsername
        Authentication authentication = authenticationManagerBuilder.getObject()
                .authenticate(authenticationToken);

        // set thông tin người dùng đăng nhập vào context (có thể sử dụng sau này)
        SecurityContextHolder.getContext().setAuthentication(authentication);

        ResLoginDTO res = new ResLoginDTO();
        User currentUserDB = this.userService.handleGetUserByUsername(loginDto.getUsername());
        if (currentUserDB != null) {
            ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin(
                    currentUserDB.getId(),
                    currentUserDB.getEmail(),
                    currentUserDB.getName(),
                    currentUserDB.getRole());
            res.setUser(userLogin);
        }

        // create access token
        String access_token = this.securityUtil.createAccessToken(authentication.getName(), res);
        res.setAccessToken(access_token);

        // create refresh token
        String refresh_token = this.securityUtil.createRefreshToken(loginDto.getUsername(), res);

        // update user
        this.userService.updateUserToken(refresh_token, loginDto.getUsername());

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
                    user.getId(), user.getEmail(), user.getName(), user.getRole());
            res.setUser(userLogin);

            // Tạo access token
            String accessToken = securityUtil.createAccessToken(email, res);
            res.setAccessToken(accessToken);

            // Tạo refresh token
            String refreshToken = securityUtil.createRefreshToken(email, res);
            userService.updateUserToken(refreshToken, email);

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
            userLogin.setRole(currentUserDB.getRole());

            userGetAccount.setUser(userLogin);
        }

        return ResponseEntity.ok().body(userGetAccount);
    }

    @GetMapping("/auth/refresh")
    @ApiMessage("Get User by refresh token")
    public ResponseEntity<ResLoginDTO> getRefreshToken(
            @CookieValue(name = "refresh_token", defaultValue = "abc") String refresh_token) throws IdInvalidException {
        if (refresh_token.equals("abc")) {
            throw new IdInvalidException("Bạn không có refresh token ở cookie");
        }
        // check valid
        Jwt decodedToken = this.securityUtil.checkValidRefreshToken(refresh_token);
        String email = decodedToken.getSubject();

        // check user by token + email
        User currentUser = this.userService.getUserByRefreshTokenAndEmail(refresh_token, email);
        if (currentUser == null) {
            throw new IdInvalidException("Refresh Token không hợp lệ");
        }

        // issue new token/set refresh token as cookies
        ResLoginDTO res = new ResLoginDTO();
        User currentUserDB = this.userService.handleGetUserByUsername(email);
        if (currentUserDB != null) {
            ResLoginDTO.UserLogin userLogin = new ResLoginDTO.UserLogin(
                    currentUserDB.getId(),
                    currentUserDB.getEmail(),
                    currentUserDB.getName(),
                    currentUserDB.getRole());
            res.setUser(userLogin);
        }

        // create access token
        String access_token = this.securityUtil.createAccessToken(email, res);
        res.setAccessToken(access_token);

        // create refresh token
        String new_refresh_token = this.securityUtil.createRefreshToken(email, res);

        // update user
        this.userService.updateUserToken(new_refresh_token, email);

        // set cookies
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

    @PostMapping("/auth/logout")
    @ApiMessage("Logout User")
    public ResponseEntity<Void> logout() throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get() : "";

        if (email.equals("")) {
            throw new IdInvalidException("Access Token không hợp lệ");
        }

        // update refresh token = null
        this.userService.updateUserToken(null, email);

        // remove refresh token cookie
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

    @PostMapping("/auth/register")
    @ApiMessage("Register a new user")
    public ResponseEntity<ResCreateUserDTO> register(@Valid @RequestBody User postManUser) throws IdInvalidException {
        // Kiểm tra password không trống
        if (postManUser.getPassword() == null || postManUser.getPassword().trim().isEmpty()) {
            throw new IdInvalidException("Password không được để trống");
        }
        boolean isEmailExist = this.userService.isEmailExist(postManUser.getEmail());
        if (isEmailExist) {
            throw new IdInvalidException(
                    "Email " + postManUser.getEmail() + "đã tồn tại, vui lòng sử dụng email khác.");
        }

        String hashPassword = this.passwordEncoder.encode(postManUser.getPassword());
        postManUser.setPassword(hashPassword);
        User ericUser = this.userService.handleCreateUser(postManUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(this.userService.convertToResCreateUserDTO(ericUser));
    }

    @PostMapping("/auth/change-password")
    @ApiMessage("Đổi mật khẩu")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ReqChangePasswordDTO changePasswordDTO)
            throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy user"));
        User user = userService.handleGetUserByUsername(email);
        if (user == null) {
            throw new IdInvalidException("User không tồn tại");
        }

        // Nếu password null (Google login), cho phép đổi mà không cần oldPassword
        if (user.getPassword() != null) {
            // Yêu cầu oldPassword nếu password không null
            if (changePasswordDTO.getOldPassword() == null
                    || !passwordEncoder.matches(changePasswordDTO.getOldPassword(), user.getPassword())) {
                throw new IdInvalidException("Mật khẩu cũ không đúng. Nếu quên mật khẩu, hãy yêu cầu OTP.");
            }
        }

        user.setPassword(passwordEncoder.encode(changePasswordDTO.getNewPassword()));
        userService.handleUpdateUser(user);
        return ResponseEntity.ok().build();
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
    public ResponseEntity<Void> verifyOtpAndChangePassword(
            @Valid @RequestBody ReqVerifyOtpChangePasswordDTO verifyOtpDTO) throws IdInvalidException {
        otpService.validateOtp(verifyOtpDTO.getEmail(), verifyOtpDTO.getOtpCode());
        User user = userService.handleGetUserByUsername(verifyOtpDTO.getEmail());
        if (user == null) {
            throw new IdInvalidException("User không tồn tại");
        }

        user.setPassword(passwordEncoder.encode(verifyOtpDTO.getNewPassword()));
        userService.handleUpdateUser(user);
        return ResponseEntity.ok().build();
    }
}
