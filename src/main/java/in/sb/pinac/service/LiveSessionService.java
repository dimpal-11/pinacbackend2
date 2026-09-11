package in.sb.pinac.service;

import in.sb.pinac.dto.LiveSessionDTO;
import in.sb.pinac.entity.Course;
import in.sb.pinac.entity.Enrollment;
import in.sb.pinac.entity.LiveSession;
import in.sb.pinac.repository.CourseRepository;
import in.sb.pinac.repository.EnrollmentRepository;
import in.sb.pinac.repository.LiveSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class LiveSessionService {

    @Autowired
    private LiveSessionRepository liveSessionRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    /**
     * Get all sessions with filters (Admin view)
     */
    public List<LiveSessionDTO> getAllSessions(String search, String status, Long courseId) {
        List<LiveSession> sessions = liveSessionRepository.findAllByOrderBySessionDateDescStartTimeDesc();

        return sessions.stream()
                .peek(LiveSession::computeDynamicStatus)
                .filter(s -> {
                    if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
                        if (!status.trim().equalsIgnoreCase(s.getStatus())) return false;
                    }
                    if (courseId != null && s.getCourse() != null) {
                        if (!s.getCourse().getId().equals(courseId)) return false;
                    }
                    if (search != null && !search.trim().isEmpty()) {
                        String q = search.trim().toLowerCase();
                        boolean matchTitle = s.getLectureTitle() != null && s.getLectureTitle().toLowerCase().contains(q);
                        boolean matchFaculty = s.getFacultyName() != null && s.getFacultyName().toLowerCase().contains(q);
                        boolean matchCourse = s.getCourse() != null && s.getCourse().getTitle() != null && s.getCourse().getTitle().toLowerCase().contains(q);
                        if (!matchTitle && !matchFaculty && !matchCourse) return false;
                    }
                    return true;
                })
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Metrics: Total, Live Now, Upcoming, Completed
     */
    public Map<String, Object> getSessionStats() {
        List<LiveSession> all = liveSessionRepository.findAll();
        long total = all.size();
        long liveNow = 0;
        long upcoming = 0;
        long completed = 0;

        for (LiveSession s : all) {
            String st = s.computeDynamicStatus();
            if ("LIVE_NOW".equalsIgnoreCase(st)) {
                liveNow++;
            } else if ("UPCOMING".equalsIgnoreCase(st)) {
                upcoming++;
            } else {
                completed++;
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("liveNow", liveNow);
        stats.put("upcoming", upcoming);
        stats.put("completed", completed);
        return stats;
    }

    /**
     * Get single session by ID
     */
    public LiveSessionDTO getSessionById(Long id) {
        LiveSession session = liveSessionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Live session not found with ID: " + id));
        session.computeDynamicStatus();
        return toDTO(session);
    }

    /**
     * Create live session & auto-link to course
     */
    public LiveSessionDTO createSession(LiveSessionDTO dto) {
        if (dto.getCourseId() == null) {
            throw new IllegalArgumentException("Course ID is required to create a live session.");
        }
        if (dto.getLectureTitle() == null || dto.getLectureTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Lecture Title is required.");
        }
        if (dto.getFacultyName() == null || dto.getFacultyName().trim().isEmpty()) {
            throw new IllegalArgumentException("Faculty Name is required.");
        }
        if (dto.getSessionDate() == null) {
            throw new IllegalArgumentException("Session Date is required.");
        }
        if (dto.getStartTime() == null || dto.getEndTime() == null) {
            throw new IllegalArgumentException("Start Time and End Time are required.");
        }
        if (dto.getZoomUrl() == null || dto.getZoomUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("Zoom Meeting URL is required.");
        }
        if (dto.getMeetingId() == null || dto.getMeetingId().trim().isEmpty()) {
            throw new IllegalArgumentException("Meeting ID is required.");
        }

        Course course = null;
        if (dto.getCourseId() != null) {
            course = courseRepository.findById(dto.getCourseId()).orElse(null);
        }
        if (course == null) {
            List<Course> allCourses = courseRepository.findAll();
            if (!allCourses.isEmpty()) {
                course = allCourses.get(0);
            }
        }
        if (course == null) {
            throw new IllegalArgumentException("No course available in database to link live session.");
        }

        LiveSession session = new LiveSession();
        session.setCourse(course);
        session.setBatch(dto.getBatch() != null && !dto.getBatch().trim().isEmpty() ? dto.getBatch().trim() : "All Batches");
        session.setLectureTitle(dto.getLectureTitle().trim());
        session.setFacultyName(dto.getFacultyName().trim());
        session.setDescription(dto.getDescription());
        session.setSessionDate(dto.getSessionDate());
        session.setStartTime(dto.getStartTime());
        session.setEndTime(dto.getEndTime());
        session.setZoomUrl(dto.getZoomUrl().trim());
        session.setMeetingId(dto.getMeetingId().trim());
        session.setPasscode(dto.getPasscode() != null ? dto.getPasscode().trim() : "");
        session.setThumbnail(dto.getThumbnail() != null && !dto.getThumbnail().trim().isEmpty() ? dto.getThumbnail().trim() : course.getThumbnail());
        session.setRecordingEnabled(dto.getRecordingEnabled() != null ? dto.getRecordingEnabled() : true);
        session.setRecordingUrl(dto.getRecordingUrl());
        session.setNotesPdfUrl(dto.getNotesPdfUrl());

        long minutes = Duration.between(dto.getStartTime(), dto.getEndTime()).toMinutes();
        session.setDurationMinutes(minutes > 0 ? (int) minutes : 60);

        session.computeDynamicStatus();
        LiveSession saved = liveSessionRepository.save(session);
        return toDTO(saved);
    }

    /**
     * Update session details
     */
    public LiveSessionDTO updateSession(Long id, LiveSessionDTO dto) {
        LiveSession session = liveSessionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Live session not found with ID: " + id));

        if (dto.getCourseId() != null && !dto.getCourseId().equals(session.getCourse().getId())) {
            Course course = courseRepository.findById(dto.getCourseId())
                    .orElseThrow(() -> new NoSuchElementException("Course not found with ID: " + dto.getCourseId()));
            session.setCourse(course);
        }

        if (dto.getBatch() != null) session.setBatch(dto.getBatch().trim());
        if (dto.getLectureTitle() != null) session.setLectureTitle(dto.getLectureTitle().trim());
        if (dto.getFacultyName() != null) session.setFacultyName(dto.getFacultyName().trim());
        if (dto.getDescription() != null) session.setDescription(dto.getDescription());
        if (dto.getSessionDate() != null) session.setSessionDate(dto.getSessionDate());
        if (dto.getStartTime() != null) session.setStartTime(dto.getStartTime());
        if (dto.getEndTime() != null) session.setEndTime(dto.getEndTime());
        if (dto.getZoomUrl() != null) session.setZoomUrl(dto.getZoomUrl().trim());
        if (dto.getMeetingId() != null) session.setMeetingId(dto.getMeetingId().trim());
        if (dto.getPasscode() != null) session.setPasscode(dto.getPasscode().trim());
        if (dto.getThumbnail() != null) session.setThumbnail(dto.getThumbnail().trim());
        if (dto.getRecordingEnabled() != null) session.setRecordingEnabled(dto.getRecordingEnabled());
        if (dto.getRecordingUrl() != null) session.setRecordingUrl(dto.getRecordingUrl().trim());
        if (dto.getNotesPdfUrl() != null) session.setNotesPdfUrl(dto.getNotesPdfUrl().trim());

        if (session.getStartTime() != null && session.getEndTime() != null) {
            long minutes = Duration.between(session.getStartTime(), session.getEndTime()).toMinutes();
            session.setDurationMinutes(minutes > 0 ? (int) minutes : 60);
        }

        session.computeDynamicStatus();
        LiveSession updated = liveSessionRepository.save(session);
        return toDTO(updated);
    }

    /**
     * Delete session
     */
    public void deleteSession(Long id) {
        if (!liveSessionRepository.existsById(id)) {
            throw new NoSuchElementException("Live session not found with ID: " + id);
        }
        liveSessionRepository.deleteById(id);
    }

    /**
     * Upload recorded lecture URL & Lecture Notes PDF
     */
    public LiveSessionDTO uploadRecording(Long id, String recordingUrl, String notesPdfUrl, String description) {
        LiveSession session = liveSessionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Live session not found with ID: " + id));

        if (recordingUrl != null && !recordingUrl.trim().isEmpty()) {
            session.setRecordingUrl(recordingUrl.trim());
        }
        if (notesPdfUrl != null && !notesPdfUrl.trim().isEmpty()) {
            session.setNotesPdfUrl(notesPdfUrl.trim());
        }
        if (description != null && !description.trim().isEmpty()) {
            session.setDescription(description.trim());
        }
        session.setRecordingEnabled(true);
        session.setStatus("COMPLETED");

        LiveSession saved = liveSessionRepository.save(session);
        return toDTO(saved);
    }

    /**
     * Student Auto-Connection:
     * Fetch all sessions belonging to courses the student is enrolled in.
     */
    public List<LiveSessionDTO> getSessionsForStudent(Long userId) {
        List<Long> enrolledCourseIds = new ArrayList<>();
        if (userId != null) {
            List<Enrollment> enrollments = enrollmentRepository.findByUserIdAndActiveTrue(userId);
            enrolledCourseIds = enrollments.stream()
                    .filter(e -> e.getCourse() != null)
                    .map(e -> e.getCourse().getId())
                    .distinct()
                    .collect(Collectors.toList());
        }

        List<LiveSession> sessions;
        if (!enrolledCourseIds.isEmpty()) {
            sessions = liveSessionRepository.findByCourseIdInOrderByDateAsc(enrolledCourseIds);
        } else {
            // If user has no active enrollments or is browsing demo, provide available public sessions
            sessions = liveSessionRepository.findAllByOrderBySessionDateDescStartTimeDesc();
        }

        return sessions.stream()
                .peek(LiveSession::computeDynamicStatus)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Current Live Course (Requirement 8)
     */
    public LiveSessionDTO getCurrentLiveSession(Long userId) {
        List<LiveSessionDTO> studentSessions = getSessionsForStudent(userId);
        return studentSessions.stream()
                .filter(s -> "LIVE_NOW".equalsIgnoreCase(s.getStatus()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Upcoming Live Sessions (Requirement 9)
     */
    public List<LiveSessionDTO> getUpcomingSessions(Long userId) {
        List<LiveSessionDTO> studentSessions = getSessionsForStudent(userId);
        return studentSessions.stream()
                .filter(s -> "UPCOMING".equalsIgnoreCase(s.getStatus()))
                .sorted(Comparator.comparing(LiveSessionDTO::getSessionDate).thenComparing(LiveSessionDTO::getStartTime))
                .collect(Collectors.toList());
    }

    /**
     * Completed Sessions with Recordings (Requirement 10 & 12)
     */
    public List<LiveSessionDTO> getCompletedSessions(Long userId) {
        List<LiveSessionDTO> studentSessions = getSessionsForStudent(userId);
        return studentSessions.stream()
                .filter(s -> "COMPLETED".equalsIgnoreCase(s.getStatus()))
                .sorted(Comparator.comparing(LiveSessionDTO::getSessionDate, Comparator.reverseOrder()).thenComparing(LiveSessionDTO::getStartTime, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    /**
     * DTO Converter
     */
    public LiveSessionDTO toDTO(LiveSession s) {
        if (s == null) return null;
        LiveSessionDTO dto = new LiveSessionDTO();
        dto.setId(s.getId());
        if (s.getCourse() != null) {
            dto.setCourseId(s.getCourse().getId());
            dto.setCourseName(s.getCourse().getTitle());
        }
        dto.setBatch(s.getBatch());
        dto.setLectureTitle(s.getLectureTitle());
        dto.setFacultyName(s.getFacultyName());
        dto.setDescription(s.getDescription());
        dto.setSessionDate(s.getSessionDate());
        dto.setStartTime(s.getStartTime());
        dto.setEndTime(s.getEndTime());
        dto.setZoomUrl(s.getZoomUrl());
        dto.setMeetingId(s.getMeetingId());
        dto.setPasscode(s.getPasscode());
        dto.setThumbnail(s.getThumbnail());
        dto.setRecordingEnabled(s.getRecordingEnabled());
        dto.setRecordingUrl(s.getRecordingUrl());
        dto.setNotesPdfUrl(s.getNotesPdfUrl());
        dto.setStatus(s.getStatus());
        dto.setDurationMinutes(s.getDurationMinutes());
        return dto;
    }
}
