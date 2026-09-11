package in.sb.pinac.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "live_sessions")
public class LiveSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "chapters", "curriculum"})
    private Course course;

    @Column(length = 100)
    private String batch = "All Batches";

    @Column(name = "lecture_title", nullable = false)
    private String lectureTitle;

    @Column(name = "faculty_name", nullable = false)
    private String facultyName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @JsonFormat(pattern = "HH:mm[:ss]")
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm[:ss]")
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "zoom_url", nullable = false, length = 1000)
    private String zoomUrl;

    @Column(name = "meeting_id", nullable = false, length = 100)
    private String meetingId;

    @Column(name = "passcode", length = 100)
    private String passcode;

    @Column(length = 1000)
    private String thumbnail;

    @Column(name = "recording_enabled")
    private Boolean recordingEnabled = true;

    @Column(name = "recording_url", length = 1000)
    private String recordingUrl;

    @Column(name = "notes_pdf_url", length = 1000)
    private String notesPdfUrl;

    @Column(length = 50)
    private String status = "UPCOMING"; // UPCOMING, LIVE_NOW, COMPLETED

    @Column(name = "duration_minutes")
    private Integer durationMinutes = 60;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public LiveSession() {
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (recordingEnabled == null) recordingEnabled = true;
        if (batch == null || batch.trim().isEmpty()) batch = "All Batches";
        computeDynamicStatus();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        computeDynamicStatus();
    }

    /**
     * Dynamically calculates status: UPCOMING, LIVE_NOW, or COMPLETED
     * based on sessionDate, startTime, and endTime compared against current time.
     */
    public String computeDynamicStatus() {
        if (sessionDate == null || startTime == null || endTime == null) {
            return this.status != null ? this.status : "UPCOMING";
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sessionStart = LocalDateTime.of(sessionDate, startTime);
        LocalDateTime sessionEnd = LocalDateTime.of(sessionDate, endTime);

        if (now.isBefore(sessionStart)) {
            this.status = "UPCOMING";
        } else if (!now.isBefore(sessionStart) && !now.isAfter(sessionEnd)) {
            this.status = "LIVE_NOW";
        } else {
            this.status = "COMPLETED";
        }
        return this.status;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public String getBatch() {
        return batch;
    }

    public void setBatch(String batch) {
        this.batch = batch;
    }

    public String getLectureTitle() {
        return lectureTitle;
    }

    public void setLectureTitle(String lectureTitle) {
        this.lectureTitle = lectureTitle;
    }

    public String getFacultyName() {
        return facultyName;
    }

    public void setFacultyName(String facultyName) {
        this.facultyName = facultyName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDate sessionDate) {
        this.sessionDate = sessionDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getZoomUrl() {
        return zoomUrl;
    }

    public void setZoomUrl(String zoomUrl) {
        this.zoomUrl = zoomUrl;
    }

    public String getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(String meetingId) {
        this.meetingId = meetingId;
    }

    public String getPasscode() {
        return passcode;
    }

    public void setPasscode(String passcode) {
        this.passcode = passcode;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public Boolean getRecordingEnabled() {
        return recordingEnabled;
    }

    public void setRecordingEnabled(Boolean recordingEnabled) {
        this.recordingEnabled = recordingEnabled;
    }

    public String getRecordingUrl() {
        return recordingUrl;
    }

    public void setRecordingUrl(String recordingUrl) {
        this.recordingUrl = recordingUrl;
    }

    public String getNotesPdfUrl() {
        return notesPdfUrl;
    }

    public void setNotesPdfUrl(String notesPdfUrl) {
        this.notesPdfUrl = notesPdfUrl;
    }

    public String getStatus() {
        computeDynamicStatus();
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
