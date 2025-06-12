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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.hoidanit.jobhunter.domain.entity.ChatMessage;
import vn.hoidanit.jobhunter.domain.entity.User;
import vn.hoidanit.jobhunter.domain.request.ChatNotificationDTO;
import vn.hoidanit.jobhunter.domain.response.RestResponse;
import vn.hoidanit.jobhunter.service.ChatMessageService;
import vn.hoidanit.jobhunter.service.UserService;

@RestController
public class ChatController {

    private final UserService userService;
    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(
            UserService userService,
            ChatMessageService chatMessageService,
            SimpMessagingTemplate messagingTemplate) {
        this.userService = userService;
        this.chatMessageService = chatMessageService;
        this.messagingTemplate = messagingTemplate;
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

    @GetMapping("/users-connected")
    public ResponseEntity<RestResponse> findConnectedUsers(@RequestParam Long id) {

        RestResponse e = new RestResponse();
        e.setData(userService.findConnectedUsers(id));
        e.setStatusCode(HttpStatus.OK.value());
        return ResponseEntity.ok(e);
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

        messagingTemplate.convertAndSendToUser(
                chatMessage.getReceiver().getEmail(),
                "/queue/messages",
                chatNotification);
    }

    @GetMapping("/messages/{senderId}/{recipientId}")
    public ResponseEntity<RestResponse> findChatMessages(
            @PathVariable Long senderId,
            @PathVariable Long recipientId) {
        List<ChatMessage> chatList = chatMessageService.findChatMessages(senderId, recipientId);
        RestResponse e = new RestResponse();
        e.setData(chatList);
        e.setStatusCode(HttpStatus.OK.value());
        return ResponseEntity
                .ok(e);
    }

}
