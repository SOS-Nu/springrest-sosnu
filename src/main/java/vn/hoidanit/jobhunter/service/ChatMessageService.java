package vn.hoidanit.jobhunter.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import vn.hoidanit.jobhunter.domain.entity.ChatMessage;
import vn.hoidanit.jobhunter.domain.response.ResChatMessageDTO;
import vn.hoidanit.jobhunter.repository.ChatMessageRepository;
import vn.hoidanit.jobhunter.service.IService.IChatMessageService;

@Service
public class ChatMessageService implements IChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomService chatRoomService;

    public ChatMessageService(
            ChatMessageRepository chatMessageRepository,
            ChatRoomService chatRoomService) {
        this.chatMessageRepository = chatMessageRepository;
        this.chatRoomService = chatRoomService;
    }

    @Override
    public ChatMessage save(ChatMessage chatMessage) {

        var chatRoom = chatRoomService.getChatRoomName(
                chatMessage.getSender().getId(),
                chatMessage.getReceiver().getId(),
                true);
        chatMessage.setRoomName(chatRoom);
        this.chatMessageRepository.save(chatMessage);
        return chatMessage;
    }

    // THAY ĐỔI PHƯƠNG THỨC NÀY
    @Override
    public List<ResChatMessageDTO> findChatMessages( // 1. Thay đổi kiểu trả về
            Long senderId,
            Long recipientId) {
        // Lấy danh sách entity từ repository
        List<ChatMessage> messages = this.chatMessageRepository.findConversation(senderId, recipientId);

        // 2. Chuyển đổi danh sách entity sang danh sách DTO
        return messages.stream()
                .map(ResChatMessageDTO::convertToDTO)
                .collect(Collectors.toList());
    }

}
