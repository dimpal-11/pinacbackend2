package in.sb.pinac.config;

import in.sb.pinac.entity.*;
import in.sb.pinac.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initDatabase(
            CourseRepository courseRepository,
            CategoryRepository categoryRepository,
            ChapterRepository chapterRepository,
            LessonRepository lessonRepository,
            UserRepository userRepository,
            CouponRepository couponRepository,
            PaymentRepository paymentRepository,
            EnrollmentRepository enrollmentRepository,
            ReviewRepository reviewRepository,
            WebsiteSettingRepository websiteSettingRepository,
            PasswordEncoder passwordEncoder,
            org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        return args -> {
            // Ensure certificates schema migration columns exist in PostgreSQL
            try {
                jdbcTemplate.execute("ALTER TABLE certificates ADD COLUMN IF NOT EXISTS student_name VARCHAR(255);");
                jdbcTemplate.execute("ALTER TABLE certificates ADD COLUMN IF NOT EXISTS course_title VARCHAR(255);");
                jdbcTemplate.execute("ALTER TABLE certificates ADD COLUMN IF NOT EXISTS completion_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP;");
                jdbcTemplate.execute("ALTER TABLE certificates ADD COLUMN IF NOT EXISTS verification_url VARCHAR(500);");

                jdbcTemplate.execute("ALTER TABLE courses ADD COLUMN IF NOT EXISTS start_date DATE;");
                jdbcTemplate.execute("ALTER TABLE courses ADD COLUMN IF NOT EXISTS end_date DATE;");
                jdbcTemplate.execute("UPDATE courses SET start_date = '2026-09-10' WHERE start_date IS NULL;");
                jdbcTemplate.execute("UPDATE courses SET end_date = '2026-12-10' WHERE end_date IS NULL;");
            } catch (Exception e) {
                System.err.println("Notice on schema alter: " + e.getMessage());
            }

            // 0. Initialize Website Settings
            if (websiteSettingRepository.count() == 0) {
                WebsiteSetting settings = new WebsiteSetting();
                settings.setWebsiteName("PINACXTREME");
                settings.setTagline("Learn Today. Build Tomorrow.");
                settings.setContactEmail("support@pinacinstitute.com");
                settings.setContactPhone("+91 72191 94211");
                settings.setAddress("PINAC Animation & VFX Academy, Nashik, Maharashtra, India");
                settings.setInstagramUrl("https://instagram.com/pinacanimation");
                settings.setYoutubeUrl("https://youtube.com/@pinacanimation");
                settings.setFacebookUrl("https://facebook.com/pinacanimation");
                settings.setLinkedinUrl("https://linkedin.com/company/pinacanimation");
                settings.setTwitterUrl("https://twitter.com/pinacanimation");
                settings.setMetaTitle("PINACXTREME - Premier 3D Animation, VFX & Game Dev Academy");
                settings.setMetaDescription("Master 3D Modeling, Texturing, Animation, VFX, and Game Development with industry veterans.");
                settings.setMetaKeywords("3D Animation, VFX, Game Development, Unreal Engine, Blender, Maya, Courses");
                websiteSettingRepository.save(settings);
            }

            // 1. Initialize Admin
            if (userRepository.findByEmail("admin@pinacinstitute.com").isEmpty()) {
                User admin = new User();
                admin.setName("PINAC Academic Admin");
                admin.setEmail("admin@pinacinstitute.com");
                admin.setMobile("9876543210");
                admin.setStudentId("PINAC-ADM-0001");
                admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
                admin.setTempPassword("Admin@123");
                admin.setRole("ADMIN");
                admin.setCity("Nashik");
                admin.setActive(true);
                admin.setCreatedAt(LocalDateTime.now().minusDays(90));
                userRepository.save(admin);
            }

            // 2. Initialize Categories
            if (categoryRepository.count() == 0) {
                List<Category> categories = Arrays.asList(
                        new Category("3D & Animation", "3d-animation", "Box", "Master 3D modeling, photorealistic rendering, and character rigging."),
                        new Category("VFX & Video", "vfx-video", "Video", "Professional video editing, color grading, chroma keying, and motion effects."),
                        new Category("Design & UI/UX", "design-ui-ux", "Layout", "Create industry-standard visual identity systems, Figma wireframes, and design tokens."),
                        new Category("Tech & Code", "tech-code", "Code", "Full-stack development with React, Spring Boot, Python, and cloud infrastructure."),
                        new Category("Game Development", "game-dev", "Gamepad", "Build AAA Unreal Engine 5 worlds, level blueprints, and game assets."),
                        new Category("AI & Data Science", "ai-data-science", "Cpu", "Machine Learning models, predictive data science, and Generative AI pipelines.")
                );
                categoryRepository.saveAll(categories);
            }

            // 3. Initialize Coupons
            if (couponRepository.count() == 0) {
                List<Coupon> coupons = Arrays.asList(
                        new Coupon("PINAC20", 20.0, null, 1000),
                        new Coupon("WELCOME500", null, 500.0, 500),
                        new Coupon("SUPERDEAL", 30.0, null, 250)
                );
                couponRepository.saveAll(coupons);
            }

            // 4. Initialize Courses with full syllabus (9 official courses)
            if (courseRepository.count() == 0) {
                Course c1 = createCourse(
                        "3D Modeling & Texturing", "3d-modeling-texturing", "3D & Animation",
                        "Learn how to create professional 3D models and realistic textures for games, films, and animation.",
                        "Learn how to create professional 3D models and realistic textures for games, films, and animation.",
                        2999.0, 2999.0, "/3d-modeling-thumbnail.jpg",
                        "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4",
                        "Pankaj Patil", "Founder & Lead Mentor", 4.9, 9420, "36h 20m", "Beginner to Intermediate", "English / Hindi", "Bestseller",
                        "[\"Create high-quality 3D models\", \"Master UV Mapping & Texturing\", \"Industry-standard workflow\", \"Beginner to Intermediate Level\"]"
                );
                courseRepository.save(c1);
                addSampleSyllabus(c1, chapterRepository, lessonRepository, "3D Modeling Fundamentals", "PBR Texturing Pipeline", "Portfolio Project & Asset Export");

                Course c2 = createCourse(
                        "3D Lighting & Rendering", "3d-lighting-rendering", "3D & Animation",
                        "Master cinematic lighting techniques and create stunning photorealistic renders.",
                        "Master cinematic lighting techniques and create stunning photorealistic renders.",
                        2999.0, 2999.0, "/3d-lighting-rendering-thumbnail.jpg",
                        "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4",
                        "Pankaj Patil", "Founder & Lead Mentor", 4.8, 6240, "28h 10m", "Intermediate", "English / Hindi", "Trending",
                        "[\"Professional Lighting Setup\", \"Realistic Rendering Techniques\", \"HDRI & Camera Basics\", \"Portfolio-Ready Projects\"]"
                );
                courseRepository.save(c2);
                addSampleSyllabus(c2, chapterRepository, lessonRepository, "Principles of Light & Optics", "Arnold & Cycles Engine Deep Dive", "Final Cinematic Render & Compositing");

                Course c3 = createCourse(
                        "3D Animation", "3d-animation", "3D & Animation",
                        "Learn the fundamentals of animation and bring 3D objects to life with smooth motion.",
                        "Learn the fundamentals of animation and bring 3D objects to life with smooth motion.",
                        2999.0, 2999.0, "/character-animation-thumbnail.jpg",
                        "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4",
                        "Pankaj Patil", "Founder & Lead Mentor", 4.9, 8850, "34h 00m", "All Levels", "English / Hindi", "Popular",
                        "[\"Animation Principles\", \"Keyframe Animation\", \"Character Rig Animation\", \"Practical Animation Projects\"]"
                );
                courseRepository.save(c3);
                addSampleSyllabus(c3, chapterRepository, lessonRepository, "Animation Principles & Timing", "Body Mechanics & Physics", "Demo Reel Staging & Polish");

                Course c4 = createCourse(
                        "3D Character Animation", "3d-character-animation", "3D & Animation",
                        "Master professional character animation techniques used in movies and AAA games.",
                        "Master professional character animation techniques used in movies and AAA games.",
                        4999.0, 4999.0, "/character-animation-thumbnail.jpg",
                        "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4",
                        "Pankaj Patil", "Founder & Lead Mentor", 4.9, 11250, "42h 45m", "Advanced", "English / Hindi", "Hot",
                        "[\"Character Rig Animation\", \"Walk & Run Cycles\", \"Facial Expressions\", \"Industry-Level Workflow\"]"
                );
                courseRepository.save(c4);
                addSampleSyllabus(c4, chapterRepository, lessonRepository, "Rigging Setup & Weight Painting", "Locomotion: Walk & Run Cycles", "Acting, Dialogue & Facial Expressions");

                Course c5 = createCourse(
                        "3D Architecture", "3d-architecture", "3D & Architecture",
                        "Create realistic architectural visualizations for homes, interiors, and commercial projects.",
                        "Create realistic architectural visualizations for homes, interiors, and commercial projects.",
                        3999.0, 3999.0, "/3d-architectural-visualization-thumbnail.jpg",
                        "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4",
                        "Pankaj Patil", "Founder & Lead Mentor", 4.8, 7400, "35h 30m", "All Levels", "English / Hindi", "Featured",
                        "[\"Interior & Exterior Design\", \"Realistic Materials\", \"Architectural Rendering\", \"Client Presentation Skills\"]"
                );
                courseRepository.save(c5);
                addSampleSyllabus(c5, chapterRepository, lessonRepository, "Architectural CAD Modeling", "Day & Night Interior Lighting", "Lumion Walkthroughs & Client Renders");

                Course c6 = createCourse(
                        "Master's In Video Editing", "masters-video-editing", "Video Editing",
                        "Become a professional video editor by learning editing, transitions, effects, and color grading.",
                        "Become a professional video editor by learning editing, transitions, effects, and color grading.",
                        999.0, 999.0, "/masters-video-editing-thumbnail.jpg",
                        "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4",
                        "Pankaj Patil", "Founder & Lead Mentor", 4.9, 16800, "38h 15m", "Beginner to Advanced", "English / Hindi", "Bestseller",
                        "[\"Professional Editing Workflow\", \"Smooth Transitions & Effects\", \"Color Correction & Grading\", \"YouTube & Social Media Editing\"]"
                );
                courseRepository.save(c6);
                addSampleSyllabus(c6, chapterRepository, lessonRepository, "Editing Workflow & Cut Techniques", "Color Grading & Node Pipelines", "Audio Mixing & Final Mastering");

                Course c7 = createCourse(
                        "Master's In Graphic Design", "masters-graphic-design", "Graphic Design",
                        "Learn professional graphic design for branding, social media, advertising, and print.",
                        "Learn professional graphic design for branding, social media, advertising, and print.",
                        999.0, 999.0, "/masters-graphic-design-thumbnail.jpg",
                        "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4",
                        "Pankaj Patil", "Founder & Lead Mentor", 4.8, 14500, "32h 00m", "All Levels", "English / Hindi", "Popular",
                        "[\"Logo & Brand Design\", \"Social Media Creatives\", \"Posters & Flyers\", \"Industry Software Training\"]"
                );
                courseRepository.save(c7);
                addSampleSyllabus(c7, chapterRepository, lessonRepository, "Typography & Vector Logo Design", "Photoshop Digital Manipulation", "Branding Guidelines & Print Packaging");

                Course c8 = createCourse(
                        "Basic VFX", "basic-vfx", "VFX",
                        "Start your VFX journey with essential compositing and visual effects techniques.",
                        "Start your VFX journey with essential compositing and visual effects techniques.",
                        2999.0, 2999.0, "https://images.unsplash.com/photo-1536240478700-b869070f9279?w=800&q=80",
                        "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4",
                        "Pankaj Patil", "Founder & Lead Mentor", 4.8, 5600, "26h 40m", "Beginner", "English / Hindi", "Trending",
                        "[\"Green Screen Editing\", \"Motion Tracking Basics\", \"Visual Effects Fundamentals\", \"Beginner-Friendly Projects\"]"
                );
                courseRepository.save(c8);
                addSampleSyllabus(c8, chapterRepository, lessonRepository, "Chroma Key & Green Screen Isolation", "Rotoscoping & Paint Clean Up", "Motion Tracking & Element Compositing");

                Course c9 = createCourse(
                        "Advance VFX", "advance-vfx", "VFX",
                        "Learn advanced visual effects used in films, advertisements, and OTT productions.",
                        "Learn advanced visual effects used in films, advertisements, and OTT productions.",
                        2999.0, 2999.0, "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800&q=80",
                        "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4",
                        "Pankaj Patil", "Founder & Lead Mentor", 4.9, 6800, "45h 00m", "Advanced", "English / Hindi", "Hot",
                        "[\"Advanced Compositing\", \"3D Camera Tracking\", \"CGI Integration\", \"Film-Level VFX Projects\"]"
                );
                courseRepository.save(c9);
                addSampleSyllabus(c9, chapterRepository, lessonRepository, "Nuke Node Architecture & EXR Passes", "3D Camera Projection & CGI Integration", "Deep Compositing, FX Integration & Film Grain");

                System.out.println("Initialized comprehensive LMS course catalog with 9 official courses, chapters & lessons.");
            }

            // 4b. Ensure all courses have chapters and lessons populated
            if (chapterRepository.count() == 0) {
                List<Course> allCourses = courseRepository.findAll();
                for (Course crs : allCourses) {
                    addSampleSyllabus(crs, chapterRepository, lessonRepository,
                            crs.getTitle() + " - Fundamentals",
                            crs.getTitle() + " - Applied Production Workflows",
                            crs.getTitle() + " - Capstone & Certification");
                }
                System.out.println("Populated chapters and lessons for all official courses.");
            }
        };
    }

    private Course createCourse(
            String title, String slug, String category, String description, String shortDescription,
            Double price, Double discountPrice, String thumbnail, String trailerVideoUrl,
            String instructor, String instructorRole, Double rating, Integer studentsCount,
            String duration, String level, String language, String badge, String highlightsJson) {

        Course c = new Course();
        c.setTitle(title);
        c.setSlug(slug);
        c.setCategory(category);
        c.setDescription(description);
        c.setShortDescription(shortDescription);
        c.setPrice(price);
        c.setDiscountPrice(discountPrice);
        c.setThumbnail(thumbnail);
        c.setTrailerVideoUrl(trailerVideoUrl);
        c.setInstructor(instructor);
        c.setInstructorRole(instructorRole);
        c.setRating(rating);
        c.setStudentsCount(studentsCount);
        c.setDuration(duration);
        c.setLevel(level);
        c.setLanguage(language);
        c.setBadge(badge);
        c.setHighlightsJson(highlightsJson);
        c.setActive(true);
        c.setCreatedAt(LocalDateTime.now());
        return c;
    }

    private void addSampleSyllabus(Course course, ChapterRepository chRepo, LessonRepository lRepo,
                                  String ch1Title, String ch2Title, String ch3Title) {
        Chapter ch1 = new Chapter(ch1Title, 1);
        ch1.setCourse(course);
        ch1 = chRepo.save(ch1);

        Lesson l1 = new Lesson("1.1 Course Introduction & Workspace Setup", "10m 15s", "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4", "Welcome to the course! Download required software tools.", "https://pinacinstitute.com/resources/setup.zip", true, 1);
        l1.setChapter(ch1);
        lRepo.save(l1);

        Lesson l2 = new Lesson("1.2 Core Fundamentals & Best Practices", "18m 40s", "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4", "Deep dive into the core principles.", "https://pinacinstitute.com/resources/cheatsheet.pdf", false, 2);
        l2.setChapter(ch1);
        lRepo.save(l2);

        Lesson l3 = new Lesson("1.3 Hands-on Exercise & Lab 1", "24m 10s", "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4", "Step-by-step practical implementation exercise.", "https://pinacinstitute.com/resources/exercise1.zip", false, 3);
        l3.setChapter(ch1);
        lRepo.save(l3);

        Chapter ch2 = new Chapter(ch2Title, 2);
        ch2.setCourse(course);
        ch2 = chRepo.save(ch2);

        Lesson l4 = new Lesson("2.1 Intermediate Concepts & Pipeline", "16m 30s", "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4", "Working with industry-standard production workflows.", "https://pinacinstitute.com/resources/part2.zip", false, 1);
        l4.setChapter(ch2);
        lRepo.save(l4);

        Lesson l5 = new Lesson("2.2 Deep Dive Project Walkthrough", "32m 00s", "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4", "Real client brief demonstration.", "https://pinacinstitute.com/resources/project-files.zip", false, 2);
        l5.setChapter(ch2);
        lRepo.save(l5);

        Chapter ch3 = new Chapter(ch3Title, 3);
        ch3.setCourse(course);
        ch3 = chRepo.save(ch3);

        Lesson l6 = new Lesson("3.1 Advanced Optimization & Polishing", "22m 15s", "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4", "Polishing techniques and performance benchmarks.", "https://pinacinstitute.com/resources/advanced.zip", false, 1);
        l6.setChapter(ch3);
        lRepo.save(l6);

        Lesson l7 = new Lesson("3.2 Final Assessment & Certificate Capstone", "28m 45s", "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4", "Complete this project to claim your Course Completion Certificate!", "https://pinacinstitute.com/resources/capstone.zip", false, 2);
        l7.setChapter(ch3);
        lRepo.save(l7);
    }
}
