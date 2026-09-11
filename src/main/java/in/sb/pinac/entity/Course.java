package in.sb.pinac.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String slug;

    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "short_description")
    private String shortDescription;

    private Double price;

    @Column(name = "discount_price")
    private Double discountPrice;

    private String thumbnail;

    private String banner;

    @Column(name = "trailer_video_url")
    private String trailerVideoUrl;

    private String instructor;

    @Column(name = "instructor_role")
    private String instructorRole;

    private Double rating = 4.8;

    @Column(name = "students_count")
    private Integer studentsCount = 5000;

    @Transient
    private Integer studentCount;

    @Transient
    private Integer purchasedCount;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    private String duration;

    private String level;

    private String language;

    private String badge; // "Bestseller", "Trending", "Hot", "Top Rated"

    @Column(name = "highlights_json", columnDefinition = "TEXT")
    private String highlightsJson;

    private Boolean active = true;

    private String status = "PUBLISHED"; // "PUBLISHED", "DRAFT", "HIDDEN"

    @Column(name = "seo_title")
    private String seoTitle;

    @Column(name = "seo_description", columnDefinition = "TEXT")
    private String seoDescription;

    @Column(name = "seo_keywords", columnDefinition = "TEXT")
    private String seoKeywords;

    @Column(name = "notes_pdf_url")
    private String notesPdfUrl;

    @Column(name = "source_zip_url")
    private String sourceZipUrl;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    private List<Chapter> chapters = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Course() {
    }

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        if (active == null) active = true;
        if (rating == null) rating = 4.8;
        if (studentsCount == null) studentsCount = 5000;
        if (slug == null && title != null) {
            slug = title.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        }
    }

    public void addChapter(Chapter chapter) {
        chapters.add(chapter);
        chapter.setCourse(this);
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getDiscountPrice() {
        return discountPrice;
    }

    public void setDiscountPrice(Double discountPrice) {
        this.discountPrice = discountPrice;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getTrailerVideoUrl() {
        return trailerVideoUrl;
    }

    public void setTrailerVideoUrl(String trailerVideoUrl) {
        this.trailerVideoUrl = trailerVideoUrl;
    }

    public String getInstructor() {
        return instructor;
    }

    public void setInstructor(String instructor) {
        this.instructor = instructor;
    }

    public String getInstructorRole() {
        return instructorRole;
    }

    public void setInstructorRole(String instructorRole) {
        this.instructorRole = instructorRole;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Integer getStudentsCount() {
        return studentsCount;
    }

    public void setStudentsCount(Integer studentsCount) {
        this.studentsCount = studentsCount;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getBadge() {
        return badge;
    }

    public void setBadge(String badge) {
        this.badge = badge;
    }

    public String getHighlightsJson() {
        return highlightsJson;
    }

    public void setHighlightsJson(String highlightsJson) {
        this.highlightsJson = highlightsJson;
    }

    public String getBanner() {
        return banner;
    }

    public void setBanner(String banner) {
        this.banner = banner;
    }

    public String getStatus() {
        return status != null ? status : (Boolean.TRUE.equals(active) ? "PUBLISHED" : "DRAFT");
    }

    public void setStatus(String status) {
        this.status = status;
        if ("PUBLISHED".equalsIgnoreCase(status)) {
            this.active = true;
        } else if ("DRAFT".equalsIgnoreCase(status) || "HIDDEN".equalsIgnoreCase(status)) {
            this.active = false;
        }
    }

    public String getSeoTitle() {
        return seoTitle;
    }

    public void setSeoTitle(String seoTitle) {
        this.seoTitle = seoTitle;
    }

    public String getSeoDescription() {
        return seoDescription;
    }

    public void setSeoDescription(String seoDescription) {
        this.seoDescription = seoDescription;
    }

    public String getSeoKeywords() {
        return seoKeywords;
    }

    public void setSeoKeywords(String seoKeywords) {
        this.seoKeywords = seoKeywords;
    }

    public String getNotesPdfUrl() {
        return notesPdfUrl;
    }

    public void setNotesPdfUrl(String notesPdfUrl) {
        this.notesPdfUrl = notesPdfUrl;
    }

    public String getSourceZipUrl() {
        return sourceZipUrl;
    }

    public void setSourceZipUrl(String sourceZipUrl) {
        this.sourceZipUrl = sourceZipUrl;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
        if (Boolean.TRUE.equals(active)) {
            this.status = "PUBLISHED";
        } else {
            this.status = "DRAFT";
        }
    }

    public List<Chapter> getChapters() {
        return chapters;
    }

    public void setChapters(List<Chapter> chapters) {
        this.chapters = chapters;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @JsonProperty("courseId")
    public Long getCourseId() {
        return id;
    }

    @JsonProperty("courseName")
    public String getCourseName() {
        return title;
    }

    @JsonProperty("studentCount")
    public Integer getStudentCount() {
        return studentCount != null ? studentCount : (studentsCount != null ? studentsCount : 0);
    }

    public void setStudentCount(Integer studentCount) {
        this.studentCount = studentCount;
        this.studentsCount = studentCount;
    }

    @JsonProperty("purchasedCount")
    public Integer getPurchasedCount() {
        return purchasedCount != null ? purchasedCount : (studentCount != null ? studentCount : 0);
    }

    public void setPurchasedCount(Integer purchasedCount) {
        this.purchasedCount = purchasedCount;
    }

    public LocalDate getStartDate() {
        if (startDate == null) {
            return LocalDate.of(2026, 9, 10);
        }
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        if (endDate == null) {
            LocalDate start = getStartDate();
            return start != null ? start.plusMonths(3) : LocalDate.of(2026, 12, 10);
        }
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
