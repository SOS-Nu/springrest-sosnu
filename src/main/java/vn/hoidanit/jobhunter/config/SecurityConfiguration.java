package vn.hoidanit.jobhunter.config;

import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.util.Base64;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import vn.hoidanit.jobhunter.service.RedisTokenBlacklistService;
import vn.hoidanit.jobhunter.service.UserService;
import vn.hoidanit.jobhunter.util.SecurityUtil;
import vn.hoidanit.jobhunter.web.filter.RateLimitingFilter;
import vn.hoidanit.jobhunter.web.filter.TokenBlacklistFilter;

@Configuration
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfiguration {

    @Value("${hoidanit.jwt.base64-secret}")
    private String jwtKey;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    String[] whiteList = {
            "/",
            "/api/v1/auth/login", "/api/v1/auth/google", "/api/v1/auth/refresh", "/storage/**",
            "/api/v1/auth/login-otp/send",
            "/api/v1/auth/login-otp/verify",
            "/api/v1/auth/logout",
            "/api/v1/auth/register/**",
            "/api/v1/companies/**", "/api/v1/jobs/**",
            "/api/v1/email/**",
            "/v3/api-docs/**",
            "/api/v1/jobs/**", // Chỉ áp dụng cho GET
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/ws/**",
            "/auth/send-otp", "/auth/verify-otp-change-password",
            "/api/v1/payment/vnpay/**",
            "/api/v1/jobs/by-company/**",
            "/api/v1/comments/**",
            "/api/v1/jobs/by-company/**",
            "/api/v1/skills/**",
            "/api/v1/users/detail/**",
            "/api/v1/gemini/**",
            "/api/v1/users/is-public",
            "/api/v1/dashboard",
            "/api/v1/files/**",
            "/api/v1/auth/send-otp",
            "/api/v1/auth/verify-otp-change-password",
            "/api/v1/users/update-own-info",
            "/api/v1/gemini/evaluate-cv/**",
            "/api/v1/notify-user/**",
            "/api/v1/gemini/search-results/**",
            "/api/v1/gemini/initiate-search/**",

    };

    @Bean
    public TokenBlacklistFilter tokenBlacklistFilter(
            RedisTokenBlacklistService blacklistService,
            RequestMatcher publicApiMatcher,
            UserService userService // Thêm tham số này
    ) {
        return new TokenBlacklistFilter(blacklistService, publicApiMatcher, userService);
    }

    @Bean
    public RequestMatcher publicApiMatcher() {
        List<RequestMatcher> matchers = List.of(whiteList)
                .stream()
                .map(AntPathRequestMatcher::new) // Dùng AntPathRequestMatcher
                .collect(Collectors.toList());
        return new OrRequestMatcher(matchers); // Gộp lại
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
            CustomAuthenticationEntryPoint customAuthenticationEntryPoint, RateLimitingFilter rateLimitingFilter,
            TokenBlacklistFilter tokenBlacklistFilter, UserService userService,
            RequestMatcher publicApiMatcher)
            throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(
                        authz -> authz
                                .requestMatchers(whiteList).permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/v1/jobs/by-user-company").authenticated()
                                .requestMatchers(HttpMethod.PUT, "/api/v1/jobs/by-user-company").authenticated()
                                .requestMatchers(HttpMethod.DELETE, "/api/v1/jobs/by-user-company/{id}").authenticated()
                                .requestMatchers(HttpMethod.POST, "/api/v1/companies/by-user").authenticated()
                                .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/v1/comments").authenticated()
                                .requestMatchers(HttpMethod.POST, "/api/v1/users/main-resume").authenticated()
                                .requestMatchers("/api/v1/online-resumes/**").authenticated()
                                .requestMatchers("/api/v1/work-experiences/**").authenticated()
                                .anyRequest().authenticated())
                .oauth2ResourceServer((oauth2) -> oauth2
                        .jwt(Customizer.withDefaults()) // Dùng JwtDecoder mặc định (sẽ được định nghĩa bên dưới)
                        .authenticationEntryPoint(customAuthenticationEntryPoint))

                //
                // // .exceptionHandling(
                // // exceptions -> exceptions
                // // .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint()) //
                // 401
                // // .accessDeniedHandler(new BearerTokenAccessDeniedHandler())) // 403
                //
                .formLogin(f -> f.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(rateLimitingFilter, BasicAuthenticationFilter.class)
                .addFilterAfter(tokenBlacklistFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public RateLimitingFilter rateLimitingFilter(
            ProxyManager<String> proxyManager,
            BucketConfiguration defaultBucketConfig,
            vn.hoidanit.jobhunter.config.RateLimitProperties props) {
        return new RateLimitingFilter(proxyManager, defaultBucketConfig, props);
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("");
        grantedAuthoritiesConverter.setAuthoritiesClaimName("permission");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(
                getSecretKey()).macAlgorithm(SecurityUtil.JWT_ALGORITHM).build();
        return token -> {
            try {
                // Chỉ làm đúng 1 nhiệm vụ: giải mã.
                return jwtDecoder.decode(token);
            } catch (Exception e) {
                System.out.println(">>> JWT error: " + e.getMessage());
                throw e;
            }
        };
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(getSecretKey()));
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.from(jwtKey).decode();
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, SecurityUtil.JWT_ALGORITHM.getName());
    }

}
