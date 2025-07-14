package vn.hoidanit.jobhunter.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.hoidanit.jobhunter.domain.entity.ChatMessage;
import vn.hoidanit.jobhunter.domain.entity.User;
import vn.hoidanit.jobhunter.domain.request.ChatNotificationDTO;
import vn.hoidanit.jobhunter.domain.response.ResChatMessageDTO;
import vn.hoidanit.jobhunter.domain.response.ResUserDTO;
import vn.hoidanit.jobhunter.domain.response.RestResponse;
import vn.hoidanit.jobhunter.service.ChatMessageService;
import vn.hoidanit.jobhunter.service.HeartbeatService;
import vn.hoidanit.jobhunter.service.UserService;
import vn.hoidanit.jobhunter.util.error.IdInvalidException;

class PingPayload {
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}

@RestController
public class ChatController {

    private final UserService userService;
    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final HeartbeatService heartbeatService; // << INJECT SERVICE

    public ChatController(
            UserService userService,
            ChatMessageService chatMessageService,
            SimpMessagingTemplate messagingTemplate, HeartbeatService heartbeatService) {
        this.userService = userService;
        this.chatMessageService = chatMessageService;
        this.messagingTemplate = messagingTemplate;
        this.heartbeatService = heartbeatService;
    }

    // update status online/ offline
    @MessageMapping("/user.addUser") // "/app/user.addUser"
    @SendTo("/user/public") // subscribe
    public User updateStatus(
            @Payload User user) {
        userService.updateStatus(user);
        return user;
    }

    @MessageMapping("/user.disconnectUser")
    @SendTo("/user/public")
    public User disconnectUser(
            @Payload User user) {
        userService.disconnect(user);
        return user;
    }

    @MessageMapping("/heartbeat.ping")
    public void handlePing(@Payload PingPayload payload) {
        if (payload != null && payload.getEmail() != null) {
            heartbeatService.ping(payload.getEmail());
        }
    }

    @GetMapping("/users-connected")
    public ResponseEntity<List<ResUserDTO>> findConnectedUsers(@RequestParam("id") Long id) throws IdInvalidException {
        // Lời gọi này bây giờ sẽ thực thi logic mới trong UserService
        List<ResUserDTO> users = userService.findConnectedUsers(id);

        // Logic kiểm tra null có thể không còn cần thiết nếu service luôn trả về một
        // list (có thể rỗng)
        if (users == null) {
            // Hoặc bạn có thể trả về một list rỗng thay vì ném exception
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }

        return ResponseEntity.ok().body(users);
    }

    @MessageMapping("/chat")
    public void processMessage(@Payload ChatMessage chatMessage) {

        User sender = this.userService.findUserById(chatMessage.getSender().getId());
        chatMessage.setSender(sender);

        User receiver = this.userService.findUserById(chatMessage.getReceiver().getId());
        chatMessage.setReceiver(receiver);

        ChatMessage savedMsg = chatMessageService.save(chatMessage);

        ChatNotificationDTO chatNotification = new ChatNotificationDTO();
        chatNotification.setId(savedMsg.getId());
        chatNotification.setContent(savedMsg.getContent());
        chatNotification.setReceiverId(savedMsg.getReceiver().getId());
        chatNotification.setSenderId(savedMsg.getSender().getId());
        chatNotification.setTimeStamp(savedMsg.getTimeStamp());

        messagingTemplate.convertAndSendToUser(
                chatMessage.getReceiver().getEmail(),
                "/queue/messages",
                chatNotification);
    }

    @GetMapping("/messages/{senderId}/{recipientId}")
    public ResponseEntity<List<ResChatMessageDTO>> findChatMessages(
            @PathVariable("senderId") Long senderId,
            @PathVariable("recipientId") Long recipientId) {
        List<ResChatMessageDTO> chatList = chatMessageService.findChatMessages(senderId, recipientId);

        return ResponseEntity.ok().body(chatList);
    }

}
