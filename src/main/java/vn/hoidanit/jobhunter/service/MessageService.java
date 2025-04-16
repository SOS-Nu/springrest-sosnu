package vn.hoidanit.jobhunter.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import vn.hoidanit.jobhunter.domain.Message;
import vn.hoidanit.jobhunter.repository.MessageRepository;

import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public Message saveMessage(Message message) {
        if (message.getSenderEmail() == null || message.getReceiverEmail() == null) {
            throw new IllegalArgumentException("Sender and receiver emails are required");
        }
        return messageRepository.save(message);
    }

    public void markMessageAsRead(long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));
        message.setIsRead(true);
        messageRepository.save(message);
    }

    public List<Message> getMessagesBetweenUsers(String senderEmail, String receiverEmail, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        return messageRepository.findBySenderEmailAndReceiverEmailOrReceiverEmailAndSenderEmail(
                senderEmail, receiverEmail, receiverEmail, senderEmail, pageable).getContent();
    }
}