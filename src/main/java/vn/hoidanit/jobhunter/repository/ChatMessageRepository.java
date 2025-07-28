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

        void deleteBySenderIdOrReceiverId(long senderId, long receiverId);

        @Query("SELECT m FROM ChatMessage m WHERE (m.sender.id = :userId1 AND m.receiver.id = :userId2) OR (m.sender.id = :userId2 AND m.receiver.id = :userId1) ORDER BY m.timeStamp DESC LIMIT 1")
        ChatMessage findLastMessageBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

        // PHƯƠNG THỨC MỚI: Lấy tin nhắn cuối cùng cho nhiều cuộc hội thoại cùng lúc
        @Query(value = """
                        WITH RankedMessages AS (
                            SELECT
                                cm.*,
                                ROW_NUMBER() OVER(
                                    PARTITION BY
                                        CASE
                                            WHEN sender_id < receiver_id THEN CONCAT(sender_id, '_', receiver_id)
                                            ELSE CONCAT(receiver_id, '_', sender_id)
                                        END
                                    ORDER BY time_stamp DESC
                                ) as rn
                            FROM chat_messages cm
                            WHERE (cm.sender_id = :userId AND cm.receiver_id IN :partnerIds)
                               OR (cm.receiver_id = :userId AND cm.sender_id IN :partnerIds)
                        )
                        SELECT * FROM RankedMessages WHERE rn = 1
                        """, nativeQuery = true)
        List<ChatMessage> findLastMessageForEachConversation(
                        @Param("userId") Long userId,
                        @Param("partnerIds") List<Long> partnerIds);

}