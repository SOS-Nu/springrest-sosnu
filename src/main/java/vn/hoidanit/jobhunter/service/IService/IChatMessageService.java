package vn.hoidanit.jobhunter.service.IService;

import java.util.List;

import vn.hoidanit.jobhunter.domain.entity.ChatMessage;
import vn.hoidanit.jobhunter.domain.response.ResChatMessageDTO;

public interface IChatMessageService {

    ChatMessage save(ChatMessage chatMessage);

    List<ResChatMessageDTO> findChatMessages(
            Long senderId,
            Long recipientId);
}
