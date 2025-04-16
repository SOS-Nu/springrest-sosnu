package vn.hoidanit.jobhunter.controller;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import vn.hoidanit.jobhunter.domain.Message;
import vn.hoidanit.jobhunter.service.MessageService;

@Controller
public class ChatController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat")
    public void sendMessage(Message message) {
        // Lưu tin nhắn vào database
        Message savedMessage = messageService.saveMessage(message);

        // Tạo destination riêng cho cặp người dùng
        String destination = createConversationDestination(
                message.getSenderEmail(), message.getReceiverEmail());

        // Gửi tin nhắn đến cả sender và receiver
        messagingTemplate.convertAndSend("/queue" + destination, savedMessage);
    }

    private String createConversationDestination(String senderEmail, String receiverEmail) {
        // Sắp xếp email để đảm bảo destination là duy nhất cho cặp người dùng
        String[] emails = new String[] { senderEmail, receiverEmail };
        Arrays.sort(emails);
        return "/conversation/" + emails[0] + "/" + emails[1];
    }
}