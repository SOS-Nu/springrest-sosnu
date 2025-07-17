package vn.hoidanit.jobhunter.config;

import java.util.Arrays;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.util.Base64;

import vn.hoidanit.jobhunter.util.SecurityUtil;

@Configuration
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfiguration {

    @Value("${hoidanit.jwt.base64-secret}")
    private String jwtKey;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
            CustomAuthenticationEntryPoint customAuthenticationEntryPoint) throws Exception {
        String[] whiteList = {
                "/",
                "/api/v1/auth/login", "/api/v1/auth/google", "/api/v1/auth/refresh", "/storage/**",
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
                "/api/v1/gemini/evaluate-cv/**"
        };
        http
                .csrf(c -> c.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(
                        authz -> authz
                                .requestMatchers(whiteList).permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/v1/jobs/by-user-company").authenticated()
                                .requestMatchers(HttpMethod.PUT, "/api/v1/jobs/by-user-company").authenticated()
                                .requestMatchers(HttpMethod.DELETE, "/api/v1/jobs/by-user-company/{id}").authenticated()
                                .requestMatchers(HttpMethod.POST, "/api/v1/companies/by-user").authenticated() // tao
                                                                                                               // company
                                                                                                               // by new
                                                                                                               // user
                                .requestMatchers(HttpMethod.POST, "/api/v1/comments").authenticated()
                                .requestMatchers(HttpMethod.POST, "/api/v1/users/main-resume").authenticated()
                                .requestMatchers("/api/v1/online-resumes/**").authenticated()
                                .requestMatchers("/api/v1/work-experiences/**").authenticated()
                                .anyRequest().authenticated())
                .oauth2ResourceServer((oauth2) -> oauth2.jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(customAuthenticationEntryPoint))

                //
                // // .exceptionHandling(
                // // exceptions -> exceptions
                // // .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint()) //
                // 401
                // // .accessDeniedHandler(new BearerTokenAccessDeniedHandler())) // 403
                //
                .formLogin(f -> f.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
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
