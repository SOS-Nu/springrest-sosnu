package vn.hoidanit.jobhunter.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.hoidanit.jobhunter.service.MessageService;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ConversationRestController {

    @Autowired
    private MessageService messageService;

    @GetMapping("/conversations")
    public List<String> getConversations(@RequestParam("userEmail") String userEmail) {
        return messageService.getConversationEmails(userEmail);
    }
}