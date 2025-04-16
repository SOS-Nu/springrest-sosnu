package vn.hoidanit.jobhunter.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import vn.hoidanit.jobhunter.domain.Message;
import vn.hoidanit.jobhunter.service.MessageService;

@Controller
public class ChatController {

    @Autowired
    private MessageService messageService;

    @MessageMapping("/chat")
    @SendTo("/topic/messages")
    @RequestMapping("/api/v1")
    public Message sendMessage(Message message) {
        return messageService.saveMessage(message);
    }
}