package in.sb.pinac.service;

import in.sb.pinac.entity.OtpVerification;
import in.sb.pinac.repository.OtpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class OtpService {

    @Autowired
    private OtpRepository otpRepository;

    /**
     * Normalize mobile number:
     * Removes non-digits, strips leading +91 / 91 if length is 12,
     * extracts standard 10-digit mobile number.
     */
    public static String normalizeMobile(String mobile) {
        if (mobile == null || mobile.trim().isEmpty()) {
            return null;
        }
        String digits = mobile.replaceAll("\\D", "").trim();
        if (digits.startsWith("91") && digits.length() == 12) {
            digits = digits.substring(2);
        } else if (digits.length() > 10) {
            digits = digits.substring(digits.length() - 10);
        }
        return digits.length() == 10 ? digits : (digits.isEmpty() ? null : digits);
    }

    /**
     * Normalize email:
     * Trims whitespace and converts to lowercase.
     */
    public static String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    /**
     * Resolve target into mobile or email.
     */
    public static String[] resolveTarget(String target, String mobile, String email) {
        String resolvedMobile = mobile;
        String resolvedEmail = email;

        if (target != null && !target.trim().isEmpty()) {
            String cleanTarget = target.trim();
            if (cleanTarget.contains("@")) {
                resolvedEmail = cleanTarget;
                if (resolvedMobile == null || resolvedMobile.isEmpty()) {
                    resolvedMobile = null;
                }
            } else {
                resolvedMobile = cleanTarget;
                if (resolvedEmail == null || resolvedEmail.isEmpty()) {
                    resolvedEmail = null;
                }
            }
        }

        return new String[]{normalizeMobile(resolvedMobile), normalizeEmail(resolvedEmail)};
    }

    /**
     * Find latest OTP record matching either email or mobile, ordered by highest ID.
     */
    public Optional<OtpVerification> findLatestOtpRecord(String cleanMobile, String cleanEmail) {
        Optional<OtpVerification> optEmail = (cleanEmail != null && !cleanEmail.isEmpty())
                ? otpRepository.findTopByEmailOrderByIdDesc(cleanEmail)
                : Optional.empty();

        Optional<OtpVerification> optMobile = (cleanMobile != null && !cleanMobile.isEmpty())
                ? otpRepository.findTopByMobileOrderByIdDesc(cleanMobile)
                : Optional.empty();

        if (optEmail.isPresent() && optMobile.isPresent()) {
            Long emailId = optEmail.get().getId() != null ? optEmail.get().getId() : 0L;
            Long mobileId = optMobile.get().getId() != null ? optMobile.get().getId() : 0L;
            return emailId >= mobileId ? optEmail : optMobile;
        }
        return optEmail.isPresent() ? optEmail : optMobile;
    }

    /**
     * Generate and save a new 6-digit OTP with 10-minute expiry.
     * Replaces and removes all previous OTP records for the target.
     */
    @Transactional
    public String generateAndSaveOtp(String mobile, String email) {
        String cleanMobile = normalizeMobile(mobile);
        String cleanEmail = normalizeEmail(email);

        if (cleanMobile == null && cleanEmail == null) {
            throw new IllegalArgumentException("Mobile number or email is required to generate OTP.");
        }

        // Clean up previous OTPs for the targets to ensure fresh state
        if (cleanMobile != null) {
            List<OtpVerification> oldMobileList = otpRepository.findByMobile(cleanMobile);
            if (!oldMobileList.isEmpty()) {
                otpRepository.deleteAll(oldMobileList);
            }
        }
        if (cleanEmail != null) {
            List<OtpVerification> oldEmailList = otpRepository.findByEmail(cleanEmail);
            if (!oldEmailList.isEmpty()) {
                otpRepository.deleteAll(oldEmailList);
            }
        }

        // Generate 6-digit numeric OTP
        String otp = String.format("%06d", new Random().nextInt(1000000));
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(10); // 10 minutes expiry window

        OtpVerification entity = new OtpVerification();
        entity.setMobile(cleanMobile);
        entity.setEmail(cleanEmail);
        entity.setOtp(otp);
        entity.setExpiresAt(expiresAt);
        entity.setVerified(false);
        entity.setConsumed(false);
        entity.setCreatedAt(now);

        otpRepository.save(entity);

        System.out.println("========================================");
        System.out.println("PINAC OTP GENERATED & STORED");
        System.out.println("Target Mobile : " + (cleanMobile != null ? cleanMobile : "N/A"));
        System.out.println("Target Email  : " + (cleanEmail != null ? cleanEmail : "N/A"));
        System.out.println("OTP Code      : " + otp);
        System.out.println("Current Time  : " + now);
        System.out.println("Expires At    : " + expiresAt);
        System.out.println("========================================");

        return otp;
    }

    /**
     * Verify input OTP against matching OTP records.
     * Marks verified=true upon success.
     */
    @Transactional
    public boolean verifyOtp(String mobile, String email, String inputOtp) {
        if (inputOtp == null || inputOtp.trim().isEmpty()) {
            return false;
        }

        String cleanMobile = normalizeMobile(mobile);
        String cleanEmail = normalizeEmail(email);
        String cleanOtp = inputOtp.trim();

        // 1. First attempt exact match on (email, otp)
        Optional<OtpVerification> matchOpt = Optional.empty();
        if (cleanEmail != null && !cleanEmail.isEmpty()) {
            matchOpt = otpRepository.findTopByEmailAndOtpOrderByIdDesc(cleanEmail, cleanOtp);
        }

        // 2. Second attempt exact match on (mobile, otp)
        if (matchOpt.isEmpty() && cleanMobile != null && !cleanMobile.isEmpty()) {
            matchOpt = otpRepository.findTopByMobileAndOtpOrderByIdDesc(cleanMobile, cleanOtp);
        }

        // 3. Fallback to latest record
        if (matchOpt.isEmpty()) {
            matchOpt = findLatestOtpRecord(cleanMobile, cleanEmail);
        }

        if (matchOpt.isEmpty()) {
            logVerifyAttempt(cleanOtp, null, false, "No OTP record found for target");
            return false;
        }

        OtpVerification entity = matchOpt.get();

        // If already consumed, reject reuse
        if (Boolean.TRUE.equals(entity.getConsumed())) {
            logVerifyAttempt(cleanOtp, entity.getOtp(), false, "OTP already consumed");
            return false;
        }

        // Validate OTP string equality
        if (!cleanOtp.equals(entity.getOtp())) {
            logVerifyAttempt(cleanOtp, entity.getOtp(), false, "OTP mismatch");
            return false;
        }

        // Check expiry (with 15-second grace window)
        if (!isNotExpired(entity.getExpiresAt())) {
            logVerifyAttempt(cleanOtp, entity.getOtp(), false, "OTP expired at " + entity.getExpiresAt());
            return false;
        }

        // Mark as verified
        entity.setVerified(true);
        entity.setVerifiedAt(LocalDateTime.now());
        otpRepository.save(entity);

        logVerifyAttempt(cleanOtp, entity.getOtp(), true, "OTP verified successfully");
        return true;
    }

    /**
     * Check if target has a verified OTP or matches the active OTP.
     * Used by signup to prevent double-verification failure.
     * Consumes the OTP so it cannot be reused.
     */
    @Transactional
    public boolean isTargetVerifiedRecently(String mobile, String email, String inputOtp) {
        String cleanMobile = normalizeMobile(mobile);
        String cleanEmail = normalizeEmail(email);
        String cleanOtp = inputOtp != null ? inputOtp.trim() : null;

        LocalDateTime now = LocalDateTime.now();

        // Strategy A: Check if email has active unexpired record with matching OTP
        if (cleanOtp != null && cleanEmail != null && !cleanEmail.isEmpty()) {
            Optional<OtpVerification> opt = otpRepository.findTopByEmailAndOtpOrderByIdDesc(cleanEmail, cleanOtp);
            if (opt.isPresent()) {
                OtpVerification entity = opt.get();
                if (!Boolean.TRUE.equals(entity.getConsumed()) && isNotExpired(entity.getExpiresAt())) {
                    entity.setVerified(true);
                    entity.setVerifiedAt(now);
                    entity.setConsumed(true);
                    otpRepository.save(entity);
                    logSignupOtpResult(cleanEmail, cleanOtp, true, "Active email OTP match");
                    return true;
                }
            }
        }

        // Strategy B: Check if mobile has active unexpired record with matching OTP
        if (cleanOtp != null && cleanMobile != null && !cleanMobile.isEmpty()) {
            Optional<OtpVerification> opt = otpRepository.findTopByMobileAndOtpOrderByIdDesc(cleanMobile, cleanOtp);
            if (opt.isPresent()) {
                OtpVerification entity = opt.get();
                if (!Boolean.TRUE.equals(entity.getConsumed()) && isNotExpired(entity.getExpiresAt())) {
                    entity.setVerified(true);
                    entity.setVerifiedAt(now);
                    entity.setConsumed(true);
                    otpRepository.save(entity);
                    logSignupOtpResult(cleanMobile, cleanOtp, true, "Active mobile OTP match");
                    return true;
                }
            }
        }

        // Strategy C: Check if email was recently verified (within last 15 minutes) and not consumed
        if (cleanEmail != null && !cleanEmail.isEmpty()) {
            Optional<OtpVerification> opt = otpRepository.findTopByEmailOrderByIdDesc(cleanEmail);
            if (opt.isPresent()) {
                OtpVerification entity = opt.get();
                if (Boolean.TRUE.equals(entity.getVerified()) && !Boolean.TRUE.equals(entity.getConsumed())) {
                    LocalDateTime cutoff = now.minusMinutes(15);
                    if (entity.getVerifiedAt() != null && entity.getVerifiedAt().isAfter(cutoff)) {
                        entity.setConsumed(true);
                        otpRepository.save(entity);
                        logSignupOtpResult(cleanEmail, cleanOtp, true, "Recently verified email OTP");
                        return true;
                    }
                }
            }
        }

        // Strategy D: Check if mobile was recently verified (within last 15 minutes) and not consumed
        if (cleanMobile != null && !cleanMobile.isEmpty()) {
            Optional<OtpVerification> opt = otpRepository.findTopByMobileOrderByIdDesc(cleanMobile);
            if (opt.isPresent()) {
                OtpVerification entity = opt.get();
                if (Boolean.TRUE.equals(entity.getVerified()) && !Boolean.TRUE.equals(entity.getConsumed())) {
                    LocalDateTime cutoff = now.minusMinutes(15);
                    if (entity.getVerifiedAt() != null && entity.getVerifiedAt().isAfter(cutoff)) {
                        entity.setConsumed(true);
                        otpRepository.save(entity);
                        logSignupOtpResult(cleanMobile, cleanOtp, true, "Recently verified mobile OTP");
                        return true;
                    }
                }
            }
        }

        // Strategy E: Check latest record for email or mobile if OTP matches
        Optional<OtpVerification> latestOpt = findLatestOtpRecord(cleanMobile, cleanEmail);
        if (latestOpt.isPresent()) {
            OtpVerification entity = latestOpt.get();
            if (cleanOtp != null && cleanOtp.equals(entity.getOtp())) {
                if (!Boolean.TRUE.equals(entity.getConsumed()) && isNotExpired(entity.getExpiresAt())) {
                    entity.setVerified(true);
                    entity.setVerifiedAt(now);
                    entity.setConsumed(true);
                    otpRepository.save(entity);
                    logSignupOtpResult(cleanEmail != null ? cleanEmail : cleanMobile, cleanOtp, true, "Latest record OTP match");
                    return true;
                }
            }
        }

        logSignupOtpResult(cleanEmail != null ? cleanEmail : cleanMobile, cleanOtp, false, "No valid/unexpired OTP match found");
        return false;
    }

    /**
     * Verify and consume OTP for Password Reset.
     */
    @Transactional
    public boolean verifyAndConsumeOtpForReset(String mobile, String email, String inputOtp) {
        boolean valid = verifyOtp(mobile, email, inputOtp);
        if (!valid) {
            return isTargetVerifiedRecently(mobile, email, inputOtp);
        }

        String cleanMobile = normalizeMobile(mobile);
        String cleanEmail = normalizeEmail(email);
        Optional<OtpVerification> opt = findLatestOtpRecord(cleanMobile, cleanEmail);

        opt.ifPresent(e -> {
            e.setConsumed(true);
            otpRepository.save(e);
        });

        return true;
    }

    /**
     * Check if expiry time is in the future, with a 15-second grace window to prevent boundary issues.
     */
    public boolean isNotExpired(LocalDateTime expiresAt) {
        if (expiresAt == null) {
            return false;
        }
        return expiresAt.plusSeconds(15).isAfter(LocalDateTime.now());
    }

    private void logVerifyAttempt(String inputOtp, String storedOtp, boolean success, String reason) {
        System.out.println("----------------------------------------");
        System.out.println("OTP VERIFY ATTEMPT");
        System.out.println("Input OTP    : " + inputOtp);
        System.out.println("Stored OTP   : " + (storedOtp != null ? storedOtp : "N/A"));
        System.out.println("Result       : " + (success ? "SUCCESS" : "FAILED (" + reason + ")"));
        System.out.println("Server Time  : " + LocalDateTime.now());
        System.out.println("----------------------------------------");
    }

    private void logSignupOtpResult(String target, String inputOtp, boolean success, String note) {
        System.out.println("----------------------------------------");
        System.out.println("SIGNUP OTP VALIDATION");
        System.out.println("Target       : " + target);
        System.out.println("Input OTP    : " + inputOtp);
        System.out.println("Result       : " + (success ? "VALID (" + note + ")" : "INVALID (" + note + ")"));
        System.out.println("Server Time  : " + LocalDateTime.now());
        System.out.println("----------------------------------------");
    }
}