package vn.hoidanit.jobhunter.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.hoidanit.jobhunter.domain.Message;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    Page<Message> findBySenderEmailAndReceiverEmailOrReceiverEmailAndSenderEmail(
            String senderEmail1, String receiverEmail1, String receiverEmail2, String senderEmail2, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE " +
            "(m.senderEmail = :senderEmail AND m.receiverEmail = :receiverEmail) OR " +
            "(m.senderEmail = :receiverEmail AND m.receiverEmail = :senderEmail) " +
            "ORDER BY m.sentAt DESC")
    List<Message> findMessagesBetweenUsers(
            @Param("senderEmail") String senderEmail,
            @Param("receiverEmail") String receiverEmail,
            Pageable pageable);

    @Query("SELECT DISTINCT m.senderEmail FROM Message m WHERE m.receiverEmail = :userEmail " +
            "UNION " +
            "SELECT DISTINCT m.receiverEmail FROM Message m WHERE m.senderEmail = :userEmail")
    List<String> findDistinctConversationEmails(@Param("userEmail") String userEmail);
}