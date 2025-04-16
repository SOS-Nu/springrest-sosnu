package vn.hoidanit.jobhunter.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.hoidanit.jobhunter.domain.Message;
import vn.hoidanit.jobhunter.service.MessageService;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class MessageRestController {

    @Autowired
    private MessageService messageService;

    @GetMapping
    public List<Message> getMessages(
            @RequestParam("sender") String senderEmail,
            @RequestParam("receiver") String receiverEmail,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        List<Message> messages = messageService.getMessagesBetweenUsers(senderEmail, receiverEmail, page, size);
        return messages != null ? messages : Collections.emptyList(); // Trả về [] nếu không tìm thấy
    }
}