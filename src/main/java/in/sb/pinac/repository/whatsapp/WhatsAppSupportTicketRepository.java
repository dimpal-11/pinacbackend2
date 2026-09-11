package in.sb.pinac.repository.whatsapp;

import in.sb.pinac.entity.whatsapp.WhatsAppSupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WhatsAppSupportTicketRepository extends JpaRepository<WhatsAppSupportTicket, Long> {

    List<WhatsAppSupportTicket> findByStatusOrderByCreatedAtDesc(String status);

    List<WhatsAppSupportTicket> findAllByOrderByCreatedAtDesc();

    long countByStatus(String status);
}
