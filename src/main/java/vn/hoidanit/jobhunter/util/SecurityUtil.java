package vn.hoidanit.jobhunter.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.util.Base64;

import vn.hoidanit.jobhunter.domain.response.ResLoginDTO;
import vn.hoidanit.jobhunter.service.RedisTokenBlacklistService;
import vn.hoidanit.jobhunter.service.UserService;

@Service
public class SecurityUtil {

    private final JwtEncoder jwtEncoder;
    private final UserService userService;

    public SecurityUtil(JwtEncoder jwtEncoder, UserService userService) {
        this.jwtEncoder = jwtEncoder;
        this.userService = userService;
    }

    public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS512;

    @Value("${hoidanit.jwt.base64-secret}")
    private String jwtKey;

    @Value("${hoidanit.jwt.access-token-validity-in-seconds}")
    private long accessTokenExpiration;

    @Value("${hoidanit.jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpiration;

    public String createAccessToken(String email, ResLoginDTO dto) {
        ResLoginDTO.UserInsideToken userToken = new ResLoginDTO.UserInsideToken();
        userToken.setId(dto.getUser().getId());
        userToken.setEmail(dto.getUser().getEmail());
        userToken.setName(dto.getUser().getName());

        Instant now = Instant.now();
        // Đảm bảo ở đây là ChronoUnit
        Instant validity = now.plus(this.accessTokenExpiration, ChronoUnit.SECONDS);

        List<String> listAuthority = this.userService.getPermissionKeysByEmail(email);
        String jti = UUID.randomUUID().toString();

        // @formatter:off
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuedAt(now)
            .expiresAt(validity)
            .subject(email)
            .claim("user", userToken)
            .claim("permission", listAuthority)
            .claim("jti", jti)
            .build();
        // @formatter:on

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    // ========== SỬA PHƯƠNG THỨC NÀY ==========
    public String createRefreshToken(String email, ResLoginDTO dto) {
        Instant now = Instant.now();

        // ========== SỬA LỖI TẠI ĐÂY ==========
        // Phải dùng refreshTokenExpiration, KHÔNG PHẢI accessTokenExpiration
        Instant validity = now.plus(this.refreshTokenExpiration, ChronoUnit.SECONDS);
        // ========== KẾT THÚC SỬA ==========

        ResLoginDTO.UserInsideToken userToken = new ResLoginDTO.UserInsideToken();
        userToken.setId(dto.getUser().getId());
        userToken.setEmail(dto.getUser().getEmail());
        userToken.setName(dto.getUser().getName());
        String jti = UUID.randomUUID().toString();

        // @formatter:off
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuedAt(now)
            .expiresAt(validity) // Dùng validity đã sửa
            .subject(email)
            .claim("user", userToken)
            .claim("jti", jti) 
            .build();
        // @formatter:on

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }
    // ========== KẾT THÚC SỬA ==========

    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.from(jwtKey).decode();
        return new SecretKeySpec(keyBytes, 0, keyBytes.length,
                JWT_ALGORITHM.getName());
    }

    public Jwt checkValidRefreshToken(String token) {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(
                getSecretKey()).macAlgorithm(SecurityUtil.JWT_ALGORITHM).build();
        try {
            return jwtDecoder.decode(token);
        } catch (Exception e) {
            System.out.println(">>> Refresh Token error: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Lấy số giây còn lại từ thời điểm hiện tại đến expiresAt.
     */
    public static long getRemainingSeconds(Instant expiresAt) {
        return RedisTokenBlacklistService.getRemainingSeconds(expiresAt);
    }

    /**
     * Get the login of the current user.
     *
     * @return the login of the current user.
     */
    public static Optional<String> getCurrentUserLogin() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(extractPrincipal(securityContext.getAuthentication()));
    }

    private static String extractPrincipal(Authentication authentication) {
        if (authentication == null) {
            return null;
        } else if (authentication.getPrincipal() instanceof UserDetails springSecurityUser) {
            return springSecurityUser.getUsername();
        } else if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        } else if (authentication.getPrincipal() instanceof String s) {
            return s;
        }
        return null;
    }

    /**
     * Get the JWT of the current user.
     *
     * @return the JWT of the current user.
     */
    public static Optional<String> getCurrentUserJWT() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(securityContext.getAuthentication())
                .filter(authentication -> authentication.getCredentials() instanceof String)
                .map(authentication -> (String) authentication.getCredentials());
    }

    /**
     * Check if a user is authenticated.
     *
     * @return true if the user is authenticated, false otherwise.
     */
    // public static boolean isAuthenticated() {
    // Authentication authentication =
    // SecurityContextHolder.getContext().getAuthentication();
    // return authentication != null &&
    // getAuthorities(authentication).noneMatch(AuthoritiesConstants.ANONYMOUS::equals);
    // }

    /**
     * Checks if the current user has any of the authorities.
     *
     * @param authorities the authorities to check.
     * @return true if the current user has any of the authorities, false otherwise.
     */
    // public static boolean hasCurrentUserAnyOfAuthorities(String... authorities) {
    // Authentication authentication =
    // SecurityContextHolder.getContext().getAuthentication();
    // return (
    // authentication != null && getAuthorities(authentication).anyMatch(authority
    // -> Arrays.asList(authorities).contains(authority))
    // );
    // }

    /**
     * Checks if the current user has none of the authorities.
     *
     * @param authorities the authorities to check.
     * @return true if the current user has none of the authorities, false
     *         otherwise.
     */
    // public static boolean hasCurrentUserNoneOfAuthorities(String... authorities)
    // {
    // return !hasCurrentUserAnyOfAuthorities(authorities);
    // }

    /**
     * Checks if the current user has a specific authority.
     *
     * @param authority the authority to check.
     * @return true if the current user has the authority, false otherwise.
     */
    // public static boolean hasCurrentUserThisAuthority(String authority) {
    // return hasCurrentUserAnyOfAuthorities(authority);
    // }

    // private static Stream<String> getAuthorities(Authentication authentication) {
    // return
    // authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority);
    // }

    /**
     * PHƯƠNG THỨC MỚI ĐƯỢC THÊM VÀO
     * Băm một chuỗi đầu vào sử dụng thuật toán MD5.
     * Dùng để tạo cache key nhất quán.
     * * @param input Chuỗi cần băm.
     * 
     * @return Chuỗi đã được băm dưới dạng hex.
     */
    public static String hash(String input) {
        if (input == null) {
            return "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashInBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hashInBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // Trong thực tế, MD5 luôn tồn tại. Exception này khó xảy ra.
            throw new RuntimeException("Không thể tìm thấy thuật toán MD5", e);
        }
    }

}
