package in.sb.pinac.service;

import in.sb.pinac.entity.*;
import in.sb.pinac.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ProgressService {

    @Autowired
    private LessonProgressRepository lessonProgressRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private CertificateRepository certificateRepository;

    public Map<String, Object> getCourseProgress(Long userId, Long courseId) {
        List<LessonProgress> progressList = lessonProgressRepository.findByUserIdAndCourseId(userId, courseId);
        long totalLessons = lessonRepository.countByCourseId(courseId);
        if (totalLessons == 0) {
            totalLessons = 7; // Fallback default lesson count
        }

        long completedCount = lessonProgressRepository.countCompletedLessonsByUserIdAndCourseId(userId, courseId);
        int percentage = (int) Math.round(((double) completedCount / totalLessons) * 100);
        percentage = Math.min(100, Math.max(0, percentage));

        Enrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(userId, courseId).orElse(null);
        if (enrollment != null && (!Objects.equals(enrollment.getProgressPercentage(), percentage) || !Objects.equals(enrollment.getCompletedLessonsCount(), (int) completedCount))) {
            enrollment.setProgressPercentage(percentage);
            enrollment.setCompletedLessonsCount((int) completedCount);
            enrollmentRepository.save(enrollment);
        }

        Set<Long> completedLessonIds = new HashSet<>();
        Map<Long, Integer> watchedMap = new HashMap<>();
        for (LessonProgress lp : progressList) {
            if (Boolean.TRUE.equals(lp.getCompleted())) {
                completedLessonIds.add(lp.getLesson().getId());
            }
            watchedMap.put(lp.getLesson().getId(), lp.getWatchedSeconds());
        }

        // Strict 100% Completion Rule:
        // Certificate is unlocked ONLY when progress is 100% (all lessons completed)
        boolean certificateEarned = (percentage >= 100 && completedCount >= totalLessons);

        // Check if certificate already exists in DB
        Optional<Certificate> existingCertOpt = certificateRepository.findByUserIdAndCourseId(userId, courseId);
        Certificate cert = existingCertOpt.orElse(null);

        if (certificateEarned && cert == null) {
            User user = userRepository.findById(userId).orElse(null);
            Course course = courseRepository.findById(courseId).orElse(null);
            if (user != null && course != null) {
                cert = certificateService.awardCertificate(user, course);
            }
        }

        Map<String, Object> res = new HashMap<>();
        res.put("courseId", courseId);
        res.put("totalLessons", totalLessons);
        res.put("completedCount", completedCount);
        res.put("progressPercentage", percentage);
        res.put("completedLessonIds", completedLessonIds);
        res.put("watchedSecondsMap", watchedMap);
        res.put("certificateEarned", cert != null);
        if (cert != null) {
            res.put("certificateCode", cert.getCertificateCode());
            res.put("certificateId", cert.getId());
            res.put("issueDate", cert.getIssueDate());
            res.put("completionDate", cert.getCompletionDate());
        }

        return res;
    }

    @Transactional
    public Map<String, Object> updateLessonProgress(Long userId, Long courseId, Long lessonId, Boolean completed, Integer watchedSeconds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));

        // Find or create lesson safely
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) {
            List<Lesson> courseLessons = lessonRepository.findAllByCourseId(courseId);
            if (!courseLessons.isEmpty()) {
                lesson = courseLessons.get(0);
            } else {
                List<Chapter> chapters = chapterRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
                Chapter chapter;
                if (chapters.isEmpty()) {
                    chapter = new Chapter("Module 1: Foundations", 1);
                    chapter.setCourse(course);
                    chapter = chapterRepository.save(chapter);
                } else {
                    chapter = chapters.get(0);
                }

                lesson = new Lesson("Lesson 1.1 - Overview", "10m 00s", "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4", "Introduction and setup.", null, true, 1);
                lesson.setChapter(chapter);
                lesson = lessonRepository.save(lesson);
            }
        }

        LessonProgress progress = lessonProgressRepository.findByUserIdAndLessonId(userId, lesson.getId())
                .orElse(new LessonProgress(user, course, lesson, false, 0));

        if (completed != null) {
            progress.setCompleted(completed);
        }
        if (watchedSeconds != null) {
            progress.setWatchedSeconds(watchedSeconds);
        }

        lessonProgressRepository.save(progress);

        // Check if certificate was already existing before this update
        boolean hadCertificateBefore = certificateRepository.existsByUserIdAndCourseId(userId, courseId);

        Map<String, Object> updatedProgress = getCourseProgress(userId, courseId);

        boolean hasCertificateNow = Boolean.TRUE.equals(updatedProgress.get("certificateEarned"));
        if (!hadCertificateBefore && hasCertificateNow) {
            updatedProgress.put("justEarned", true);
            updatedProgress.put("message", "Congratulations! You have successfully completed the course. Your certificate is now available.");
        }

        return updatedProgress;
    }

    /**
     * Explicit course completion endpoint
     */
    @Transactional
    public Map<String, Object> completeCourseDirect(Long userId, Long courseId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));

        // Mark all lessons completed for this course
        List<Lesson> lessons = lessonRepository.findAllByCourseId(courseId);
        for (Lesson l : lessons) {
            LessonProgress lp = lessonProgressRepository.findByUserIdAndLessonId(userId, l.getId())
                    .orElse(new LessonProgress(user, course, l, false, 0));
            lp.setCompleted(true);
            lessonProgressRepository.save(lp);
        }

        Certificate cert = certificateService.awardCertificate(user, course);

        Map<String, Object> res = getCourseProgress(userId, courseId);
        res.put("success", true);
        res.put("justEarned", true);
        res.put("certificateId", cert.getId());
        res.put("certificateCode", cert.getCertificateCode());
        res.put("message", "Congratulations! You have successfully completed the course. Your certificate is now available.");
        return res;
    }
}
