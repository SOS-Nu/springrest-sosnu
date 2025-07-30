// package vn.hoidanit.jobhunter.service;
//
// import org.springframework.messaging.simp.SimpMessagingTemplate;
// import org.springframework.scheduling.annotation.Scheduled;
// import org.springframework.stereotype.Service;
//
// import vn.hoidanit.jobhunter.domain.entity.User;
// import vn.hoidanit.jobhunter.util.constant.UserStatusEnum;
//
// import java.util.List;
// import java.util.Map;
// import java.util.Set;
// import java.util.concurrent.ConcurrentHashMap;
//
// @Service
// public class HeartbeatService {
//
// // Sử dụng ConcurrentHashMap để đảm bảo an toàn khi truy cập từ nhiều luồng
// private final Map<String, Long> userLastPing = new ConcurrentHashMap<>();
//
// private final UserService userService;
// private final SimpMessagingTemplate messagingTemplate;
//
// // Thời gian timeout (60 giây) tính bằng mili-giây cho WebSocket ping
// private static final long PING_TIMEOUT_MS = 60000;
//
// public HeartbeatService(UserService userService, SimpMessagingTemplate
// messagingTemplate) {
// this.userService = userService;
// this.messagingTemplate = messagingTemplate;
// }
//
// /**
// * Ghi nhận lại thời gian ping cuối cùng của một user qua WebSocket.
// *
// * @param userEmail Email của người dùng đã ping.
// */
// public void ping(String userEmail) {
// // Khi người dùng ping, cập nhật trạng thái của họ thành ONLINE
// User user = userService.handleGetUserByUsername(userEmail);
// if (user != null && user.getStatus() != UserStatusEnum.ONLINE) {
// userService.updateStatus(user);
// // Gửi thông báo user đã online trở lại
// messagingTemplate.convertAndSend("/user/public", user);
// }
// userLastPing.put(userEmail, System.currentTimeMillis());
// }
//
// /**
// * Tác vụ chạy định kỳ mỗi 60 giây để kiểm tra các kết nối WebSocket đã hết
// hạn.
// */
// @Scheduled(fixedRate = PING_TIMEOUT_MS)
// public void checkHeartbeats() {
// long now = System.currentTimeMillis();
//
// // Duyệt qua tất cả user đang được theo dõi trong map (kết nối WebSocket)
// userLastPing.forEach((email, lastPing) -> {
// if (now - lastPing > PING_TIMEOUT_MS) {
// System.out.println("User timed out (WebSocket): " + email);
// User user = userService.handleGetUserByUsername(email);
//
// if (user != null && user.getStatus() == UserStatusEnum.ONLINE) {
// userService.disconnect(user);
// messagingTemplate.convertAndSend("/user/public", user);
// }
//
// // Xóa user khỏi danh sách theo dõi WebSocket
// userLastPing.remove(email);
// }
// });
// }
//
// // ---------- START: PHƯƠNG THỨC MỚI BẠN YÊU CẦU ----------
// /**
// * Tác vụ chạy định kỳ mỗi giờ để dọn dẹp các user bị kẹt trạng thái ONLINE.
// * Cron expression "0 0 * * * *" = chạy vào phút 0, giây 0 của mỗi giờ.
// */
// @Scheduled(cron = "0 0 2 * * *") // Chạy vào 2:00:00 sáng mỗi ngày
// public void cleanupStaleOnlineUsers() {
// System.out.println("Running hourly cleanup for stale online users...");
//
// // B1: Lấy danh sách tất cả user có trạng thái ONLINE từ DB
// List<User> onlineUsersInDB = this.userService.findAllOnlineUsers();
//
// // B2: Lấy danh sách email của các user đang thực sự hoạt động (có ping
// // WebSocket)
// Set<String> activeUserEmails = userLastPing.keySet();
//
// // B3: Lọc ra những user "bị kẹt": có status ONLINE trong DB nhưng không có
// // trong
// // danh sách active WebSocket
// for (User user : onlineUsersInDB) {
// if (!activeUserEmails.contains(user.getEmail())) {
// System.out.println("Cleaning up stale user: " + user.getEmail());
//
// // B4: Cập nhật trạng thái thành OFFLINE và thông báo cho các client khác
// userService.disconnect(user);
// messagingTemplate.convertAndSend("/user/public", user);
// }
// }
// }
//
// }