package vn.hoidanit.jobhunter.config;

import java.util.Collections;
import java.util.HashSet; // Thêm
import java.util.Set; // Thêm
import java.util.stream.Collectors; // Thêm

import org.springframework.security.core.GrantedAuthority; // Thêm
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import vn.hoidanit.jobhunter.service.UserService;

@Component("userDetailService")
public class UserDetailsCustom implements UserDetailsService {

    private final UserService userService;

    public UserDetailsCustom(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Dùng phương thức đã join sẵn role và permission
        vn.hoidanit.jobhunter.domain.entity.User user = this.userService
                .findUserWithRoleAndPermissionsByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("UserName/Password không hợp lệ"));

        // Lấy tất cả quyền (bao gồm tên Role và các Permission)
        Set<GrantedAuthority> authorities = new HashSet<>();

        if (user.getRole() != null) {
            // Thêm tên Role (ví dụ: "ROLE_ADMIN")
            authorities.add(new SimpleGrantedAuthority(user.getRole().getName()));

            // Thêm các permissions (ví dụ: "CREATE:USER")
            if (user.getRole().getPermissions() != null) {
                user.getRole().getPermissions().forEach(p -> {
                    authorities.add(new SimpleGrantedAuthority(
                            (p.getMethod() + ":" + p.getApiPath()).toUpperCase()));
                });
            }
        }

        // Nếu user không có quyền nào (kể cả role), gán quyền mặc định (ví dụ
        // ROLE_USER)
        // tùy theo logic nghiệp vụ của bạn. Ở đây tôi để trống nếu không có gì.
        if (authorities.isEmpty()) {
            // Hoặc bạn có thể gán 1 role mặc định nếu muốn
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return new User(
                user.getEmail(),
                user.getPassword(),
                authorities); // Truyền vào danh sách quyền đã lấy được
    }
}