package in.sb.pinac.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "lessons")
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    @JsonIgnore
    private Chapter chapter;

    @Column(nullable = false)
    private String title;

    private String duration; // e.g. "12m 30s"

    @Column(name = "video_url")
    private String videoUrl;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "resource_url")
    private String resourceUrl;

    @Column(name = "is_free_preview")
    private Boolean isFreePreview = false;

    @Column(name = "order_index")
    private Integer orderIndex = 0;

    public Lesson() {
    }

    public Lesson(String title, String duration, String videoUrl, String notes, String resourceUrl, Boolean isFreePreview, Integer orderIndex) {
        this.title = title;
        this.duration = duration;
        this.videoUrl = videoUrl;
        this.notes = notes;
        this.resourceUrl = resourceUrl;
        this.isFreePreview = isFreePreview != null ? isFreePreview : false;
        this.orderIndex = orderIndex != null ? orderIndex : 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Chapter getChapter() {
        return chapter;
    }

    public void setChapter(Chapter chapter) {
        this.chapter = chapter;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getResourceUrl() {
        return resourceUrl;
    }

    public void setResourceUrl(String resourceUrl) {
        this.resourceUrl = resourceUrl;
    }

    public Boolean getIsFreePreview() {
        return isFreePreview;
    }

    public void setIsFreePreview(Boolean isFreePreview) {
        this.isFreePreview = isFreePreview;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }
}
