package in.sb.pinac.repository;

import in.sb.pinac.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByUserId(Long userId);
    List<Payment> findByCourseId(Long courseId);
    Optional<Payment> findByOrderId(String orderId);
    Optional<Payment> findByPaymentId(String paymentId);
    Optional<Payment> findByInvoiceNumber(String invoiceNumber);
    List<Payment> findByOrderByCreatedAtDesc();
    List<Payment> findByPaymentStatusOrderByCreatedAtDesc(String paymentStatus);
    long countByPaymentStatusIgnoreCase(String paymentStatus);
    List<Payment> findByCreatedAtBetweenOrderByCreatedAtDesc(java.time.LocalDateTime start, java.time.LocalDateTime end);
}

