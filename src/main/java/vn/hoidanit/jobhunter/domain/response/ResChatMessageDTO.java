package vn.hoidanit.jobhunter.domain.response;

import java.util.Date;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;
import vn.hoidanit.jobhunter.domain.entity.ChatMessage;
import vn.hoidanit.jobhunter.domain.entity.User;
import vn.hoidanit.jobhunter.util.constant.UserStatusEnum;

@Getter
@Setter
public class ResChatMessageDTO {
    private long id;
    private String content;
    private Date timeStamp;
    private UserInChatDTO sender;
    private UserInChatDTO receiver;

    // Lớp nội (nested class) để biểu diễn thông tin người dùng một cách đơn giản
    @Getter
    @Setter
    public static class UserInChatDTO {
        private long id;
        private String name;
        private String email;

        @Enumerated(EnumType.STRING)
        private UserStatusEnum status;
    }

    // Phương thức chuyển đổi từ Entity sang DTO
    public static ResChatMessageDTO convertToDTO(ChatMessage message) {
        ResChatMessageDTO dto = new ResChatMessageDTO();
        dto.setId(message.getId());
        dto.setContent(message.getContent());
        dto.setTimeStamp(message.getTimeStamp());

        // Chuyển đổi thông tin người gửi
        User senderEntity = message.getSender();
        if (senderEntity != null) {
            UserInChatDTO senderDTO = new UserInChatDTO();
            senderDTO.setId(senderEntity.getId());
            senderDTO.setName(senderEntity.getName());
            senderDTO.setEmail(senderEntity.getEmail());
            senderDTO.setStatus(senderEntity.getStatus());
            dto.setSender(senderDTO);
        }

        // Chuyển đổi thông tin người nhận
        User receiverEntity = message.getReceiver();
        if (receiverEntity != null) {
            UserInChatDTO receiverDTO = new UserInChatDTO();
            receiverDTO.setId(receiverEntity.getId());
            receiverDTO.setName(receiverEntity.getName());
            receiverDTO.setEmail(receiverEntity.getEmail());
            receiverDTO.setStatus(receiverEntity.getStatus());

            dto.setReceiver(receiverDTO);
        }

        return dto;
    }
}