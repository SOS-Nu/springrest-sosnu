package vn.hoidanit.jobhunter.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.hoidanit.jobhunter.domain.entity.ChatMessage;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByRoomName(String chatroomName);

    // Cập nhật phương thức này bằng cách thêm @Param
    @Query("SELECT m FROM ChatMessage m WHERE " +
            "(m.sender.id = :senderId AND m.receiver.id = :recipientId) OR " +
            "(m.sender.id = :recipientId AND m.receiver.id = :senderId) " +
            "ORDER BY m.timeStamp ASC")
    List<ChatMessage> findConversation(
            @Param("senderId") Long senderId,
            @Param("recipientId") Long recipientId);
}