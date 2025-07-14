package vn.hoidanit.jobhunter.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import vn.hoidanit.jobhunter.domain.entity.User;
import vn.hoidanit.jobhunter.util.constant.UserStatusEnum;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class HeartbeatService {

    // Sử dụng ConcurrentHashMap để đảm bảo an toàn khi truy cập từ nhiều luồng
    private final Map<String, Long> userLastPing = new ConcurrentHashMap<>();

    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    // Thời gian timeout (30 giây) tính bằng mili-giây
    private static final long PING_TIMEOUT_MS = 6000;

    public HeartbeatService(UserService userService, SimpMessagingTemplate messagingTemplate) {
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Ghi nhận lại thời gian ping cuối cùng của một user.
     *
     * @param userEmail Email của người dùng đã ping.
     */
    public void ping(String userEmail) {
        userLastPing.put(userEmail, System.currentTimeMillis());
    }

    /**
     * Tác vụ chạy định kỳ mỗi 30 giây để kiểm tra các kết nối đã hết hạn.
     */
    @Scheduled(fixedRate = PING_TIMEOUT_MS)
    public void checkHeartbeats() {
        long now = System.currentTimeMillis();

        // Duyệt qua tất cả user đang được theo dõi
        userLastPing.forEach((email, lastPing) -> {
            // Nếu thời gian ping cuối cùng đã quá thời gian timeout
            if (now - lastPing > PING_TIMEOUT_MS) {
                System.out.println("User timed out: " + email);
                User user = userService.handleGetUserByUsername(email);

                // Kiểm tra xem user có tồn tại và đang online không
                if (user != null && user.getStatus() == UserStatusEnum.ONLINE) {
                    // Cập nhật trạng thái trong DB
                    userService.disconnect(user);

                    // Phát thông báo offline cho các client khác
                    messagingTemplate.convertAndSend("/user/public", user);
                }

                // Xóa user khỏi danh sách theo dõi
                userLastPing.remove(email);
            }
        });
    }
}