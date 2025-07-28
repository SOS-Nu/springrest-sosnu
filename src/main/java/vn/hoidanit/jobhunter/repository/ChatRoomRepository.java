package vn.hoidanit.jobhunter.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.hoidanit.jobhunter.domain.entity.ChatRoom;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    ChatRoom findBySenderIdAndReceiverId(Long senderId, Long receiverId);

    List<ChatRoom> findBySenderId(Long senderId);

    List<ChatRoom> findBySenderIdOrReceiverId(Long senderId, Long receiverId);

    void deleteBySenderIdOrReceiverId(long senderId, long receiverId);

    // CẬP NHẬT PHƯƠNG THỨC NÀY
    // @Query("SELECT cr FROM ChatRoom cr JOIN FETCH cr.sender JOIN FETCH
    // cr.receiver WHERE cr.sender.id = :userId OR cr.receiver.id = :userId") //
    // Query cũ
    @Query("""
                SELECT cr FROM ChatRoom cr
                JOIN FETCH cr.sender s LEFT JOIN FETCH s.company LEFT JOIN FETCH s.role
                JOIN FETCH cr.receiver r LEFT JOIN FETCH r.company LEFT JOIN FETCH r.role
                WHERE s.id = :userId OR r.id = :userId
            """) // Query mới tối ưu triệt để
    List<ChatRoom> findBySenderOrReceiverIdWithUsers(@Param("userId") Long userId);
}
