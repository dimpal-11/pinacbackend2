package in.sb.pinac.repository.whatsapp;

import in.sb.pinac.entity.whatsapp.WhatsAppMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WhatsAppMessageRepository extends JpaRepository<WhatsAppMessage, Long> {

    List<WhatsAppMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    Optional<WhatsAppMessage> findByMessageId(String messageId);

    boolean existsByMessageId(String messageId);

    long countBySender(String sender);

    @Query("SELECT COUNT(m) FROM WhatsAppMessage m WHERE m.createdAt >= :since")
    long countMessagesSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(m) FROM WhatsAppMessage m WHERE m.createdAt >= :start AND m.createdAt < :end")
    long countMessagesBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT m.intent, COUNT(m) FROM WhatsAppMessage m WHERE m.intent IS NOT NULL GROUP BY m.intent ORDER BY COUNT(m) DESC")
    List<Object[]> findTopIntents();

    @Query("SELECT EXTRACT(HOUR FROM m.createdAt) as hr, COUNT(m) FROM WhatsAppMessage m GROUP BY hr ORDER BY hr ASC")
    List<Object[]> findMessageDistributionByHour();
}
