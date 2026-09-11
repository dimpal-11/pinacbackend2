package in.sb.pinac.repository.whatsapp;

import in.sb.pinac.entity.whatsapp.WhatsAppKnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WhatsAppKnowledgeBaseRepository extends JpaRepository<WhatsAppKnowledgeBase, Long> {

    List<WhatsAppKnowledgeBase> findByActiveTrue();

    List<WhatsAppKnowledgeBase> findByCategoryAndActiveTrue(String category);

    List<WhatsAppKnowledgeBase> findAllByOrderByCreatedAtDesc();
}
