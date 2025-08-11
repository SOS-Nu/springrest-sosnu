package vn.hoidanit.jobhunter.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.hoidanit.jobhunter.domain.entity.Permission;
import vn.hoidanit.jobhunter.domain.entity.Role;
import vn.hoidanit.jobhunter.domain.entity.User;
import vn.hoidanit.jobhunter.service.UserService;
import vn.hoidanit.jobhunter.util.SecurityUtil;
import vn.hoidanit.jobhunter.util.error.IdInvalidException;
import vn.hoidanit.jobhunter.util.error.PermissionException;

public class PermissionInterceptor implements HandlerInterceptor {

    @Autowired
    UserService userService;

    // PermissionInterceptor.java
    // PermissionInterceptor.java
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String pathPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String method = request.getMethod();

        System.out.println(">>> RUN preHandle");
        System.out.println(">>> path= " + pathPattern);
        System.out.println(">>> httpMethod= " + method);
        System.out.println(">>> requestURI= " + request.getRequestURI());

        String email = SecurityUtil.getCurrentUserLogin().orElse("");
        if (!email.isEmpty()) {
            // lấy từ cache; lần đầu MISS -> DB 1 query (JOIN FETCH), các lần sau HIT
            List<String> keys = userService.getPermissionKeysByEmail(email);
            String currentKey = (method + ":" + pathPattern).toUpperCase();
            if (!keys.contains(currentKey)) {
                throw new PermissionException("Bạn không có quyền truy cập endpoint này.");
            }
        }

        return true;
    }

}
