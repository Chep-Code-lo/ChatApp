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