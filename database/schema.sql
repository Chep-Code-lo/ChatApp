CREATE DATABASE IF NOT EXISTS chat_app CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE chat_app;

CREATE TABLE languages(
    code VARCHAR(35) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL
)   ENGINE = InnoDB;
INSERT INTO languages VALUES ('vi', 'Tiếng Việt'), ('en', 'English'), ('ja', '日本語'), ('ko', '한국어'), ('zh-Hans', '简体中文'), ('zn-Hant', '繁體中文'),('fr','Français'),('de','Deutsch'),('th','ไทย'),('es','Español'),('und','Không xác định');

CREATE TABLE users(
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(255) CHARACTER SET ascii NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    avatar_key VARCHAR(512),
    status ENUM('ACTIVE','DISABLED') NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
)   ENGINE=InnoDB;

CREATE TABLE user_settings (
    user_id BIGINT UNSIGNED PRIMARY KEY,
    preferred_language VARCHAR(35) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'vi',
    translation_prompt_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (preferred_language) REFERENCES languages(code),
    CHECK (preferred_language <> 'und'),
    CHECK (translation_prompt_enabled IN (0,1))
)   ENGINE=InnoDB;

CREATE TABLE conversations (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    type ENUM('DIRECT','GROUP') NOT NULL,
    title VARCHAR(150),
    created_by BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    FOREIGN KEY (created_by) REFERENCES users(id),
    CHECK (type <> 'GROUP' OR (title IS NOT NULL AND CHAR_LENGTH(TRIM(title)) > 0))
)   ENGINE=InnoDB;

CREATE TABLE conversation_members (
    conversation_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    role ENUM('OWNER','ADMIN','MEMBER') NOT NULL DEFAULT 'MEMBER',
    joined_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    left_at DATETIME(3),
    muted BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (conversation_id,user_id),
    KEY idx_member_user (user_id,left_at,conversation_id),
    FOREIGN KEY (conversation_id) REFERENCES conversations(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    CHECK (left_at IS NULL OR left_at >= joined_at),
    CHECK (muted IN (0,1))
)   ENGINE=InnoDB;

CREATE TABLE direct_conversations (
    conversation_id BIGINT UNSIGNED PRIMARY KEY,
    user_low BIGINT UNSIGNED NOT NULL,
    user_high BIGINT UNSIGNED NOT NULL,
    UNIQUE KEY uq_direct_pair (user_low,user_high),
    FOREIGN KEY (conversation_id,user_low) REFERENCES conversation_members(conversation_id,user_id),
    FOREIGN KEY (conversation_id,user_high) REFERENCES conversation_members(conversation_id,user_id),
    CHECK (user_low < user_high)
)   ENGINE=InnoDB;

CREATE TABLE messages (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT UNSIGNED NOT NULL,
    sender_id BIGINT UNSIGNED NOT NULL,
    client_message_id CHAR(36) CHARACTER SET ascii NOT NULL,
    type ENUM('TEXT','IMAGE','FILE','AUDIO','VIDEO') NOT NULL DEFAULT 'TEXT',
    original_text TEXT,
    detected_language VARCHAR(35) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'und',
    detection_status ENUM('PENDING','DETECTED','UNKNOWN','NOT_APPLICABLE') NOT NULL DEFAULT 'PENDING',
    detection_confidence DECIMAL(4,3),
    content_version INT UNSIGNED NOT NULL DEFAULT 1,
    reply_to_id BIGINT UNSIGNED,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    edited_at DATETIME(3),
    deleted_at DATETIME(3),
    UNIQUE KEY uq_client_message (sender_id,client_message_id),
    UNIQUE KEY uq_message_conversation (conversation_id,id),
    KEY idx_message_page (conversation_id,id),
    FOREIGN KEY (conversation_id,sender_id) REFERENCES conversation_members(conversation_id,user_id),
    FOREIGN KEY (detected_language) REFERENCES languages(code),
    FOREIGN KEY (conversation_id,reply_to_id) REFERENCES messages(conversation_id,id),
    CHECK (content_version > 0),
    CHECK (detection_confidence IS NULL OR detection_confidence BETWEEN 0 AND 1),
    CHECK (type <> 'TEXT' OR (original_text IS NOT NULL AND CHAR_LENGTH(TRIM(original_text)) > 0)),
    CHECK (detection_status <> 'DETECTED' OR detected_language <> 'und')
)   ENGINE=InnoDB;

CREATE TABLE message_attachments (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    message_id BIGINT UNSIGNED NOT NULL,
    storage_key VARCHAR(512) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    mime_type VARCHAR(127) NOT NULL,
    size_bytes BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    FOREIGN KEY (message_id) REFERENCES messages(id),
    KEY idx_attachment_message (message_id)
)   ENGINE=InnoDB;

CREATE TABLE message_receipts (
    conversation_id BIGINT UNSIGNED NOT NULL,
    message_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    delivered_at DATETIME(3),
    read_at DATETIME(3),
    PRIMARY KEY (message_id,user_id),
    FOREIGN KEY (conversation_id,message_id) REFERENCES messages(conversation_id,id),
    FOREIGN KEY (conversation_id,user_id) REFERENCES conversation_members(conversation_id,user_id),
    CHECK (read_at IS NULL OR (delivered_at IS NOT NULL AND read_at >= delivered_at))
)   ENGINE=InnoDB;

CREATE TABLE message_translations (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    message_id BIGINT UNSIGNED NOT NULL,
    content_version INT UNSIGNED NOT NULL,
    target_language VARCHAR(35) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    source_language VARCHAR(35) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    translated_text TEXT NOT NULL,
    provider VARCHAR(80) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uq_translation (message_id,content_version,target_language),
    FOREIGN KEY (message_id) REFERENCES messages(id),
    FOREIGN KEY (target_language) REFERENCES languages(code),
    FOREIGN KEY (source_language) REFERENCES languages(code),
    CHECK (content_version > 0),
    CHECK (target_language <> 'und'),
    CHECK (CHAR_LENGTH(TRIM(translated_text)) > 0)
)   ENGINE=InnoDB;

CREATE TABLE translation_choices (
    message_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    content_version INT UNSIGNED NOT NULL,
    target_language VARCHAR(35) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    choice ENUM('TRANSLATED','DISMISSED') NOT NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (message_id,user_id,content_version,target_language),
    FOREIGN KEY (message_id) REFERENCES messages(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (target_language) REFERENCES languages(code),
    CHECK (content_version > 0),
    CHECK (target_language <> 'und')
)   ENGINE=InnoDB;

CREATE TABLE user_blocks (
    blocker_id BIGINT UNSIGNED NOT NULL,
    blocked_id BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (blocker_id,blocked_id),
    FOREIGN KEY (blocker_id) REFERENCES users(id),
    FOREIGN KEY (blocked_id) REFERENCES users(id),
    CHECK (blocker_id <> blocked_id)
)   ENGINE=InnoDB;

DELIMITER   $$
CREATE TRIGGER messages_before_update BEFORE UPDATE ON messages FOR EACH ROW
BEGIN
    IF NOT (NEW.original_text <=> OLD.original_text) OR NEW.type <> OLD.type THEN
        SET NEW.content_version = OLD.content_version + 1;
        SET NEW.detected_language = 'und';
        SET NEW.detection_status = IF(NEW.original_text IS NULL, 'NOT_APPLICABLE', 'PENDING');
        SET NEW.detection_confidence = NULL;
        SET NEW.edited_at = CURRENT_TIMESTAMP(3);
    ELSE
        SET NEW.content_version = OLD.content_version;
    END IF;
END$$
DELIMITER   ;