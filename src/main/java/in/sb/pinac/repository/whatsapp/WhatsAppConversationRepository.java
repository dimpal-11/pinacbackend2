package in.sb.pinac.repository.whatsapp;

import in.sb.pinac.entity.whatsapp.WhatsAppConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WhatsAppConversationRepository extends JpaRepository<WhatsAppConversation, Long> {

    Optional<WhatsAppConversation> findByWaId(String waId);

    List<WhatsAppConversation> findByStatusOrderByLastMessageTimeDesc(String status);

    List<WhatsAppConversation> findAllByOrderByLastMessageTimeDesc();

    @Query("SELECT c FROM WhatsAppConversation c WHERE " +
           "(:status IS NULL OR :status = 'ALL' OR c.status = :status) AND " +
           "(:search IS NULL OR LOWER(c.userName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.userPhone) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.waId) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY c.lastMessageTime DESC")
    Page<WhatsAppConversation> searchConversations(@Param("status") String status,
                                                  @Param("search") String search,
                                                  Pageable pageable);

    long countByStatus(String status);

    @Query("SELECT COUNT(c) FROM WhatsAppConversation c WHERE c.createdAt >= :since")
    long countSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(c) FROM WhatsAppConversation c WHERE c.aiResolved = true")
    long countAiResolved();

    @Query("SELECT COUNT(c) FROM WhatsAppConversation c WHERE c.status = 'HUMAN_REQUIRED'")
    long countHumanRequired();
}
