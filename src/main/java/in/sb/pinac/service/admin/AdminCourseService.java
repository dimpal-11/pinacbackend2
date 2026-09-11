package in.sb.pinac.service.admin;

import in.sb.pinac.dto.admin.AdminCourseRequest;
import in.sb.pinac.entity.Chapter;
import in.sb.pinac.entity.Course;
import in.sb.pinac.entity.Lesson;
import in.sb.pinac.repository.ChapterRepository;
import in.sb.pinac.repository.CourseRepository;
import in.sb.pinac.repository.LessonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminCourseService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired(required = false)
    private in.sb.pinac.repository.EnrollmentRepository enrollmentRepository;

    @Autowired(required = false)
    private in.sb.pinac.repository.PaymentRepository paymentRepository;

    /**
     * Get all courses with optional search, category, and status filtering.
     */
    public List<Course> getAllCourses(String search, String category, String status) {
        List<Course> courses = courseRepository.findByOrderByCreatedAtDesc();

        courses.forEach(c -> {
            try {
                long count = 0;
                if (enrollmentRepository != null && c.getId() != null) {
                    count = enrollmentRepository.countDistinctActiveUsersByCourseId(c.getId());
                }
                if (count == 0 && paymentRepository != null && c.getId() != null) {
                    List<in.sb.pinac.entity.Payment> payments = paymentRepository.findByCourseId(c.getId());
                    if (payments != null) {
                        count = payments.stream()
                                .filter(p -> "SUCCESS".equalsIgnoreCase(p.getPaymentStatus()) && p.getUser() != null)
                                .map(p -> p.getUser().getId())
                                .distinct()
                                .count();
                    }
                }
                c.setPurchasedCount((int) count);
                c.setStudentCount((int) count);
            } catch (Exception ignored) {
            }
        });

        return courses.stream()
                .filter(c -> {
                    if (search == null || search.trim().isEmpty()) return true;
                    String q = search.trim().toLowerCase();
                    boolean matchTitle = c.getTitle() != null && c.getTitle().toLowerCase().contains(q);
                    boolean matchInstructor = c.getInstructor() != null && c.getInstructor().toLowerCase().contains(q);
                    boolean matchDesc = c.getDescription() != null && c.getDescription().toLowerCase().contains(q);
                    return matchTitle || matchInstructor || matchDesc;
                })
                .filter(c -> {
                    if (category == null || category.trim().isEmpty() || "ALL".equalsIgnoreCase(category)) return true;
                    return c.getCategory() != null && c.getCategory().equalsIgnoreCase(category.trim());
                })
                .filter(c -> {
                    if (status == null || status.trim().isEmpty() || "ALL".equalsIgnoreCase(status)) return true;
                    if ("PUBLISHED".equalsIgnoreCase(status)) return Boolean.TRUE.equals(c.getActive());
                    if ("DRAFT".equalsIgnoreCase(status)) return !Boolean.TRUE.equals(c.getActive());
                    return true;
                })
                .collect(Collectors.toList());
    }

    public Course getCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Course not found with ID: " + id));
    }

    /**
     * Create a new course with media and chapters.
     */
    @Transactional
    public Course createCourse(AdminCourseRequest req) {
        if (req.getTitle() == null || req.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Course title is required.");
        }

        Course course = new Course();
        mapDtoToCourse(req, course);

        if (course.getSlug() == null || course.getSlug().isEmpty()) {
            String slug = course.getTitle().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
            course.setSlug(slug);
        }

        Course saved = courseRepository.save(course);

        // Save Chapters & Lessons if provided
        if (req.getChapters() != null && !req.getChapters().isEmpty()) {
            for (AdminCourseRequest.ChapterDto chDto : req.getChapters()) {
                Chapter chapter = new Chapter();
                chapter.setTitle(chDto.getTitle() != null ? chDto.getTitle() : "Module");
                chapter.setOrderIndex(chDto.getOrderIndex() != null ? chDto.getOrderIndex() : 1);
                chapter.setCourse(saved);
                Chapter savedChapter = chapterRepository.save(chapter);

                if (chDto.getLessons() != null) {
                    for (AdminCourseRequest.LessonDto lDto : chDto.getLessons()) {
                        Lesson lesson = new Lesson();
                        lesson.setTitle(lDto.getTitle() != null ? lDto.getTitle() : "Lesson");
                        lesson.setDuration(lDto.getDuration() != null ? lDto.getDuration() : "10:00");
                        lesson.setVideoUrl(lDto.getVideoUrl());
                        lesson.setNotes(lDto.getNotes());
                        lesson.setResourceUrl(lDto.getResourceUrl());
                        lesson.setIsFreePreview(Boolean.TRUE.equals(lDto.getIsFreePreview()));
                        lesson.setOrderIndex(lDto.getOrderIndex() != null ? lDto.getOrderIndex() : 1);
                        lesson.setChapter(savedChapter);
                        lessonRepository.save(lesson);
                    }
                }
            }
        }

        return courseRepository.findById(saved.getId()).orElse(saved);
    }

    /**
     * Update an existing course.
     */
    @Transactional
    public Course updateCourse(Long id, AdminCourseRequest req) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Course not found with ID: " + id));

        mapDtoToCourse(req, course);
        return courseRepository.save(course);
    }

    /**
     * Update course status (PUBLISHED, DRAFT, HIDDEN).
     */
    @Transactional
    public Course updateCourseStatus(Long id, String status) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Course not found with ID: " + id));

        course.setStatus(status);
        if ("PUBLISHED".equalsIgnoreCase(status)) {
            course.setActive(true);
        } else {
            course.setActive(false);
        }

        return courseRepository.save(course);
    }

    /**
     * Delete a course.
     */
    @Transactional
    public void deleteCourse(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new NoSuchElementException("Course not found with ID: " + id);
        }
        courseRepository.deleteById(id);
    }

    private void mapDtoToCourse(AdminCourseRequest req, Course course) {
        if (req.getTitle() != null) course.setTitle(req.getTitle().trim());
        if (req.getSlug() != null) course.setSlug(req.getSlug().trim());
        if (req.getCategory() != null) course.setCategory(req.getCategory().trim());
        if (req.getDescription() != null) course.setDescription(req.getDescription().trim());
        if (req.getShortDescription() != null) course.setShortDescription(req.getShortDescription().trim());
        if (req.getPrice() != null) course.setPrice(req.getPrice());
        if (req.getDiscountPrice() != null) course.setDiscountPrice(req.getDiscountPrice());
        if (req.getThumbnail() != null) course.setThumbnail(req.getThumbnail().trim());
        if (req.getBanner() != null) course.setBanner(req.getBanner().trim());
        if (req.getTrailerVideoUrl() != null) course.setTrailerVideoUrl(req.getTrailerVideoUrl().trim());
        if (req.getInstructor() != null) course.setInstructor(req.getInstructor().trim());
        if (req.getInstructorRole() != null) course.setInstructorRole(req.getInstructorRole().trim());
        if (req.getRating() != null) course.setRating(req.getRating());
        if (req.getStudentsCount() != null) course.setStudentsCount(req.getStudentsCount());
        if (req.getDuration() != null) course.setDuration(req.getDuration().trim());
        if (req.getLevel() != null) course.setLevel(req.getLevel().trim());
        if (req.getLanguage() != null) course.setLanguage(req.getLanguage().trim());
        if (req.getBadge() != null) course.setBadge(req.getBadge().trim());
        if (req.getHighlightsJson() != null) course.setHighlightsJson(req.getHighlightsJson());
        if (req.getActive() != null) course.setActive(req.getActive());
        if (req.getStatus() != null) course.setStatus(req.getStatus());
        if (req.getSeoTitle() != null) course.setSeoTitle(req.getSeoTitle().trim());
        if (req.getSeoDescription() != null) course.setSeoDescription(req.getSeoDescription().trim());
        if (req.getSeoKeywords() != null) course.setSeoKeywords(req.getSeoKeywords().trim());
        if (req.getNotesPdfUrl() != null) course.setNotesPdfUrl(req.getNotesPdfUrl().trim());
        if (req.getSourceZipUrl() != null) course.setSourceZipUrl(req.getSourceZipUrl().trim());
    }
}
