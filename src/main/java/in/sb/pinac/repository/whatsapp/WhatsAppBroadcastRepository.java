package in.sb.pinac.repository.whatsapp;

import in.sb.pinac.entity.whatsapp.WhatsAppBroadcast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WhatsAppBroadcastRepository extends JpaRepository<WhatsAppBroadcast, Long> {

    List<WhatsAppBroadcast> findAllByOrderByCreatedAtDesc();

    List<WhatsAppBroadcast> findByStatus(String status);
}
