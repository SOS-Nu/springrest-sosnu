package vn.hoidanit.jobhunter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PermissionInterceptorConfiguration implements WebMvcConfigurer {
    @Bean
    PermissionInterceptor getPermissionInterceptor() {
        return new PermissionInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        String[] whiteList = {
                "/", "/api/v1/auth/**", "/storage/**",
                "/api/v1/companies/**", "/api/v1/jobs/**", "/api/v1/skills/**",
                "/api/v1/files",
                "/api/v1/resumes/**",
                "/api/v1/subscribers/**",
                "/api/v1/payment/vnpay/**",
                "/api/v1/payment/history/**",
                // chatting
                "/chat",
                "/messages/**",
                "/users-connected/**",
                "/user.disconnectUser",
                "/heartbeat.ping",
                "/user.addUser",
                "/api/v1/comments/**",
                "/api/v1/users/main-resume",
                "/api/v1/online-resumes/**",
                "/api/v1/work-experiences/**",
                "/api/v1/users/detail/**",
                "/api/v1/gemini/**",
                "/api/v1/users/is-public",
                "/api/v1/dashboard",
                "/api/v1/auth/register/**",
                "/api/v1/files/**",
                "/api/v1/auth/send-otp",
                "/api/v1/auth/verify-otp-change-password",
                "/api/v1/users/update-own-info",
                "/api/v1/gemini/evaluate-cv/**",
                "/api/v1/gemini/search-results/**",
                "/api/v1/gemini/initiate-search/**",
                "/api/v1/gemini/candidate-search-results/**",
                "/api/v1/gemini/initiate-candidate-search/**",

        };
        registry.addInterceptor(getPermissionInterceptor())
                .excludePathPatterns(whiteList);
    }
}
