package in.sb.pinac.repository.whatsapp;

import in.sb.pinac.entity.whatsapp.WhatsAppFaq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WhatsAppFaqRepository extends JpaRepository<WhatsAppFaq, Long> {

    List<WhatsAppFaq> findByActiveTrueOrderByOrderIndexAsc();

    List<WhatsAppFaq> findByCategoryAndActiveTrueOrderByOrderIndexAsc(String category);

    List<WhatsAppFaq> findAllByOrderByOrderIndexAsc();
}
