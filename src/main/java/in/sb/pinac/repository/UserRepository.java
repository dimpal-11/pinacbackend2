package in.sb.pinac.repository;

import in.sb.pinac.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findFirstByEmailOrderByIdDesc(String email);
    Optional<User> findByMobile(String mobile);
    Optional<User> findFirstByMobileOrderByIdDesc(String mobile);
    Optional<User> findByStudentId(String studentId);
    Optional<User> findByStudentIdIgnoreCase(String studentId);
    Optional<User> findFirstByStudentIdOrderByIdDesc(String studentId);
    boolean existsByEmail(String email);
    boolean existsByMobile(String mobile);
    List<User> findByRole(String role);
    List<User> findByRoleOrderByCreatedAtDesc(String role);
    List<User> findByOrderByCreatedAtDesc();
    long countByRole(String role);
    long countByRoleAndActive(String role, Boolean active);
    long countByActiveTrue();
    long countByCreatedAtAfter(java.time.LocalDateTime dateTime);
}
