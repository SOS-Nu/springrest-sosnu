package vn.hoidanit.jobhunter.service.IService;

import java.util.List;

import vn.hoidanit.jobhunter.domain.entity.ChatMessage;

public interface IChatMessageService {

    ChatMessage save(ChatMessage chatMessage);

    List<ChatMessage> findChatMessages(
            Long senderId,
            Long recipientId);
}
