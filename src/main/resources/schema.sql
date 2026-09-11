CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE,
    mobile VARCHAR(50) UNIQUE,
    student_id VARCHAR(50) UNIQUE,
    password_hash VARCHAR(255),
    temp_password VARCHAR(255),
    role VARCHAR(50) DEFAULT 'STUDENT',
    avatar VARCHAR(500),
    city VARCHAR(100),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    slug VARCHAR(255) UNIQUE,
    icon VARCHAR(100),
    description TEXT
);

CREATE TABLE IF NOT EXISTS courses (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE,
    category VARCHAR(100),
    description TEXT,
    short_description TEXT,
    price DOUBLE PRECISION NOT NULL,
    discount_price DOUBLE PRECISION,
    thumbnail VARCHAR(500),
    trailer_video_url VARCHAR(500),
    instructor VARCHAR(255),
    instructor_role VARCHAR(255),
    rating DOUBLE PRECISION DEFAULT 4.8,
    students_count INTEGER DEFAULT 0,
    duration VARCHAR(50),
    level VARCHAR(50) DEFAULT 'All Levels',
    language VARCHAR(50) DEFAULT 'English / Hindi',
    badge VARCHAR(50),
    highlights_json TEXT,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chapters (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    order_index INTEGER DEFAULT 1,
    course_id BIGINT REFERENCES courses(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS lessons (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    duration VARCHAR(50),
    video_url VARCHAR(500),
    notes TEXT,
    resource_url VARCHAR(500),
    is_free_preview BOOLEAN DEFAULT FALSE,
    order_index INTEGER DEFAULT 1,
    chapter_id BIGINT REFERENCES chapters(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(100),
    payment_id VARCHAR(100),
    invoice_number VARCHAR(100) UNIQUE,
    razorpay_signature VARCHAR(255),
    amount DOUBLE PRECISION NOT NULL,
    currency VARCHAR(10) DEFAULT 'INR',
    payment_status VARCHAR(50) DEFAULT 'PENDING',
    payment_method VARCHAR(50) DEFAULT 'RAZORPAY',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT REFERENCES users(id),
    course_id BIGINT REFERENCES courses(id)
);

CREATE TABLE IF NOT EXISTS enrollments (
    id BIGSERIAL PRIMARY KEY,
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE,
    progress_percentage INTEGER DEFAULT 0,
    completed_lessons_count INTEGER DEFAULT 0,
    user_id BIGINT REFERENCES users(id),
    course_id BIGINT REFERENCES courses(id),
    payment_id BIGINT REFERENCES payments(id)
);

CREATE TABLE IF NOT EXISTS lesson_progress (
    id BIGSERIAL PRIMARY KEY,
    completed BOOLEAN DEFAULT FALSE,
    watched_seconds INTEGER DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT REFERENCES users(id),
    course_id BIGINT REFERENCES courses(id),
    lesson_id BIGINT REFERENCES lessons(id)
);

CREATE TABLE IF NOT EXISTS certificates (
    id BIGSERIAL PRIMARY KEY,
    certificate_code VARCHAR(100) UNIQUE,
    issue_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT REFERENCES users(id),
    course_id BIGINT REFERENCES courses(id)
);

CREATE TABLE IF NOT EXISTS reviews (
    id BIGSERIAL PRIMARY KEY,
    rating DOUBLE PRECISION DEFAULT 5.0,
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT REFERENCES users(id),
    course_id BIGINT REFERENCES courses(id)
);

CREATE TABLE IF NOT EXISTS coupons (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    discount_percent DOUBLE PRECISION,
    discount_amount DOUBLE PRECISION,
    max_uses INTEGER DEFAULT 1000,
    used_count INTEGER DEFAULT 0,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS otp_verifications (
    id BIGSERIAL PRIMARY KEY,
    mobile VARCHAR(50),
    email VARCHAR(255),
    otp VARCHAR(10) NOT NULL,
    expires_at TIMESTAMP,
    expiry_time TIMESTAMP,
    verified BOOLEAN DEFAULT FALSE,
    verified_at TIMESTAMP,
    consumed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE users ALTER COLUMN email DROP NOT NULL;
ALTER TABLE otp_verifications ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP;
ALTER TABLE otp_verifications ADD COLUMN IF NOT EXISTS verified_at TIMESTAMP;
ALTER TABLE otp_verifications ADD COLUMN IF NOT EXISTS consumed BOOLEAN DEFAULT FALSE;

-- Certificates table extensions
ALTER TABLE certificates ADD COLUMN IF NOT EXISTS student_name VARCHAR(255);
ALTER TABLE certificates ADD COLUMN IF NOT EXISTS course_title VARCHAR(255);
ALTER TABLE certificates ADD COLUMN IF NOT EXISTS completion_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE certificates ADD COLUMN IF NOT EXISTS verification_url VARCHAR(500);

-- Website Settings Table for Admin Configuration
CREATE TABLE IF NOT EXISTS website_settings (
    id BIGSERIAL PRIMARY KEY,
    website_name VARCHAR(255) DEFAULT 'PINACXTREME',
    tagline VARCHAR(255) DEFAULT 'Learn Today. Build Tomorrow.',
    logo_url VARCHAR(500),
    favicon_url VARCHAR(500),
    contact_email VARCHAR(255) DEFAULT 'support@pinacinstitute.com',
    contact_phone VARCHAR(50) DEFAULT '+91 72191 94211',
    address TEXT DEFAULT 'PINAC Animation & VFX Academy, Nashik, Maharashtra, India',
    facebook_url VARCHAR(255),
    instagram_url VARCHAR(255),
    youtube_url VARCHAR(255),
    linkedin_url VARCHAR(255),
    twitter_url VARCHAR(255),
    razorpay_key_id VARCHAR(255),
    razorpay_key_secret VARCHAR(255),
    smtp_host VARCHAR(255) DEFAULT 'smtp.gmail.com',
    smtp_port INTEGER DEFAULT 587,
    smtp_username VARCHAR(255),
    smtp_password VARCHAR(255),
    smtp_from_email VARCHAR(255) DEFAULT 'pinacanimation@gmail.com',
    meta_title VARCHAR(255) DEFAULT 'PINACXTREME - Premier 3D Animation, VFX & Game Dev Academy',
    meta_description TEXT DEFAULT 'Master 3D Modeling, Texturing, Animation, VFX, and Game Development with industry veterans.',
    meta_keywords TEXT DEFAULT '3D Animation, VFX, Game Development, Unreal Engine, Blender, Maya, Courses',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Admin extensions for courses
ALTER TABLE courses ADD COLUMN IF NOT EXISTS banner VARCHAR(500);
ALTER TABLE courses ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'PUBLISHED';
ALTER TABLE courses ADD COLUMN IF NOT EXISTS seo_title VARCHAR(255);
ALTER TABLE courses ADD COLUMN IF NOT EXISTS seo_description TEXT;
ALTER TABLE courses ADD COLUMN IF NOT EXISTS seo_keywords TEXT;
ALTER TABLE courses ADD COLUMN IF NOT EXISTS notes_pdf_url VARCHAR(500);
ALTER TABLE courses ADD COLUMN IF NOT EXISTS source_zip_url VARCHAR(500);

-- Admin extensions for reviews
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'APPROVED';

-- ============================================================================
-- WhatsApp AI Chatbot Schema
-- ============================================================================

CREATE TABLE IF NOT EXISTS whatsapp_conversations (
    id BIGSERIAL PRIMARY KEY,
    wa_id VARCHAR(50) UNIQUE NOT NULL,
    user_name VARCHAR(255),
    user_phone VARCHAR(50),
    status VARCHAR(50) DEFAULT 'AI_ACTIVE',
    last_message_text TEXT,
    last_message_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    unread_count INTEGER DEFAULT 0,
    ai_resolved BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS whatsapp_messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT REFERENCES whatsapp_conversations(id) ON DELETE CASCADE,
    sender VARCHAR(50) NOT NULL,
    message_id VARCHAR(255),
    message_text TEXT NOT NULL,
    status VARCHAR(50) DEFAULT 'SENT',
    intent VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS whatsapp_knowledge_base (
    id BIGSERIAL PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    category VARCHAR(100) DEFAULT 'GENERAL',
    content TEXT NOT NULL,
    keywords VARCHAR(500),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS whatsapp_faqs (
    id BIGSERIAL PRIMARY KEY,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    category VARCHAR(100) DEFAULT 'GENERAL',
    order_index INTEGER DEFAULT 1,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS whatsapp_support_tickets (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT REFERENCES whatsapp_conversations(id) ON DELETE CASCADE,
    user_name VARCHAR(255),
    user_phone VARCHAR(50),
    reason TEXT,
    status VARCHAR(50) DEFAULT 'PENDING',
    admin_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS whatsapp_broadcasts (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    message_template TEXT NOT NULL,
    target_audience VARCHAR(100) DEFAULT 'ALL',
    total_recipients INTEGER DEFAULT 0,
    success_count INTEGER DEFAULT 0,
    failed_count INTEGER DEFAULT 0,
    status VARCHAR(50) DEFAULT 'DRAFT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS whatsapp_settings (
    id BIGSERIAL PRIMARY KEY,
    phone_number_id VARCHAR(255),
    business_account_id VARCHAR(255),
    access_token TEXT,
    verify_token VARCHAR(255) DEFAULT 'pinac_wa_webhook_verify_token_2026',
    ai_enabled BOOLEAN DEFAULT TRUE,
    gemini_api_key VARCHAR(255),
    ai_model VARCHAR(100) DEFAULT 'gemini-1.5-flash',
    auto_reply_enabled BOOLEAN DEFAULT TRUE,
    human_handover_keywords VARCHAR(500) DEFAULT 'human, sir, support, call, talk to human, agent, counselor',
    welcome_message TEXT DEFAULT 'Hello 👋 Welcome to PINAC Institute. How can I help you today?',
    fallback_message TEXT DEFAULT 'I couldn''t find the exact information. Your message has been forwarded to our support team. They will contact you shortly.',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- Student Attendance Schema
-- ============================================================================
CREATE TABLE IF NOT EXISTS attendance (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    student_id VARCHAR(50),
    student_name VARCHAR(255) NOT NULL,
    student_email VARCHAR(255),
    course_id BIGINT REFERENCES courses(id) ON DELETE SET NULL,
    course_name VARCHAR(255),
    batch_name VARCHAR(100) DEFAULT 'Batch A',
    attendance_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'PRESENT',
    check_in_time TIME,
    check_out_time TIME,
    remarks VARCHAR(500),
    marked_by VARCHAR(50) DEFAULT 'ADMIN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_att_date ON attendance(attendance_date);
CREATE INDEX IF NOT EXISTS idx_att_student_id ON attendance(student_id);
CREATE INDEX IF NOT EXISTS idx_att_course_id ON attendance(course_id);
CREATE INDEX IF NOT EXISTS idx_att_status ON attendance(status);



