package vn.hoidanit.jobhunter.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.hoidanit.jobhunter.domain.entity.ChatRoom;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    ChatRoom findBySenderIdAndReceiverId(Long senderId, Long receiverId);

    List<ChatRoom> findBySenderId(Long senderId);

    List<ChatRoom> findBySenderIdOrReceiverId(Long senderId, Long receiverId);

}
