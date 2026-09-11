package in.sb.pinac.service;

import in.sb.pinac.entity.Category;
import in.sb.pinac.entity.Chapter;
import in.sb.pinac.entity.Course;
import in.sb.pinac.entity.Lesson;
import in.sb.pinac.repository.CategoryRepository;
import in.sb.pinac.repository.ChapterRepository;
import in.sb.pinac.repository.CourseRepository;
import in.sb.pinac.repository.LessonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private in.sb.pinac.repository.EnrollmentRepository enrollmentRepository;

    public Course enrichCourseDynamicData(Course course) {
        if (course == null) return null;
        if (course.getId() != null && enrollmentRepository != null) {
            long count = enrollmentRepository.countDistinctActiveUsersByCourseId(course.getId());
            course.setStudentCount((int) count);
        }
        return course;
    }

    private List<Course> enrichCourses(List<Course> courses) {
        if (courses != null) {
            for (Course c : courses) {
                enrichCourseDynamicData(c);
            }
        }
        return courses;
    }

    public List<Course> getAllActiveCourses() {
        return enrichCourses(courseRepository.findByActiveTrue());
    }

    public List<Course> getCoursesByCategory(String category) {
        if (category == null || category.equalsIgnoreCase("All")) {
            return enrichCourses(courseRepository.findByActiveTrue());
        }
        return enrichCourses(courseRepository.findByCategoryAndActiveTrue(category));
    }

    public List<Course> searchCourses(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return enrichCourses(courseRepository.findByActiveTrue());
        }
        return enrichCourses(courseRepository.searchCourses(keyword.trim()));
    }

    public Optional<Course> getCourseById(Long id) {
        return courseRepository.findById(id).map(this::enrichCourseDynamicData);
    }

    public Optional<Course> getCourseBySlug(String slug) {
        return courseRepository.findBySlug(slug).map(this::enrichCourseDynamicData);
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findByActiveTrue();
    }

    public Map<String, Object> getCourseSyllabus(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found with ID: " + courseId));

        List<Chapter> chapters = chapterRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        long totalLessonsCount = lessonRepository.countByCourseId(courseId);

        Map<String, Object> syllabus = new HashMap<>();
        syllabus.put("courseId", course.getId());
        syllabus.put("courseTitle", course.getTitle());
        syllabus.put("category", course.getCategory());
        syllabus.put("totalChapters", chapters.size());
        syllabus.put("totalLessons", totalLessonsCount);

        List<Map<String, Object>> chapterList = new ArrayList<>();
        for (Chapter chapter : chapters) {
            Map<String, Object> chMap = new HashMap<>();
            chMap.put("id", chapter.getId());
            chMap.put("title", chapter.getTitle());
            chMap.put("orderIndex", chapter.getOrderIndex());

            List<Lesson> lessons = lessonRepository.findByChapterIdOrderByOrderIndexAsc(chapter.getId());
            List<Map<String, Object>> lessonList = new ArrayList<>();
            for (Lesson lesson : lessons) {
                Map<String, Object> lMap = new HashMap<>();
                lMap.put("id", lesson.getId());
                lMap.put("title", lesson.getTitle());
                lMap.put("duration", lesson.getDuration());
                lMap.put("videoUrl", lesson.getVideoUrl());
                lMap.put("notes", lesson.getNotes());
                lMap.put("resourceUrl", lesson.getResourceUrl());
                lMap.put("isFreePreview", lesson.getIsFreePreview());
                lMap.put("orderIndex", lesson.getOrderIndex());
                lessonList.add(lMap);
            }
            chMap.put("lessons", lessonList);
            chapterList.add(chMap);
        }

        syllabus.put("chapters", chapterList);
        return syllabus;
    }

    public Course saveCourse(Course course) {
        return courseRepository.save(course);
    }

    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }
}
