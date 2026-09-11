package in.sb.pinac.repository;

import in.sb.pinac.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByMobileOrderByIdDesc(String mobile);

    Optional<OtpVerification> findTopByEmailOrderByIdDesc(String email);

    Optional<OtpVerification> findTopByMobileAndOtpOrderByIdDesc(String mobile, String otp);

    Optional<OtpVerification> findTopByEmailAndOtpOrderByIdDesc(String email, String otp);

    Optional<OtpVerification> findTopByMobileOrderByCreatedAtDesc(String mobile);

    Optional<OtpVerification> findTopByEmailOrderByCreatedAtDesc(String email);

    List<OtpVerification> findByMobile(String mobile);

    List<OtpVerification> findByEmail(String email);

    List<OtpVerification> findByMobileAndVerifiedFalse(String mobile);

    List<OtpVerification> findByEmailAndVerifiedFalse(String email);
}
