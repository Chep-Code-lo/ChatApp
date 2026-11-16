CREATE DATABASE IF NOT EXISTS chat_app CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

USE chat_app;

SET
    NAMES utf8mb4;

SET
    time_zone = '+00:00';

SET
    FOREIGN_KEY_CHECKS = 0;

CREATE TABLE languages(
    code VARCHAR(35) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (code)
) ENGINE = InnoDB;

INSERT INTO
    languages
VALUES
    ('vi', 'Tiếng Việt'),
    ('en', 'English'),
    ('ja', '日本語'),
    ('ko', '한국어'),
    ('zh-Hans', '简体中文'),
    ('zn-Hant', '繁體中文'),
    ('fr', 'Français'),
    ('de', 'Deutsch'),
    ('th', 'ไทย'),
    ('es', 'Español'),
    ('und', 'Không xác định') ON DUPLICATE KEY
UPDATE
    display_name =
VALUES
    (display_name);

CREATE TABLE users(
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    phone_number VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'E.164, đã chuẩn hóa (+84901234567)',
    email VARCHAR(254) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL COMMENT 'Email nhận mã OTP xác thực tài khoản và khôi phục mật khẩu',
    password_hash VARCHAR(255) CHARACTER SET ascii NOT NULL COMMENT 'bcrypt hoặc Argon2',
    display_name VARCHAR(100) NOT NULL,
    bio VARCHAR(200) NULL,
    avatar_file_id BIGINT UNSIGNED NULL,
    status ENUM('PENDING', 'ACTIVE', 'DISABLED') NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING: Tài khoản đang chờ xác thực OTP qua email và tự động xóa sau 24 giờ nếu chưa xác thực',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uq_user_phone (phone_number),
    UNIQUE KEY uq_user_email (email),
    KEY idx_user_status_created (status, created_at),
    CONSTRAINT ck_user_phone CHECK (phone_number REGEXP '^\\+[1-9][0-9]{7,14}$'),
    CONSTRAINT ck_user_name CHECK (CHAR_LENGTH(TRIM(display_name)) >= 1)
) ENGINE = InnoDB;

CREATE TABLE user_settings (
    user_id BIGINT UNSIGNED NOT NULL,
    app_language VARCHAR(35) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'vi',
    translation_language VARCHAR(35) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT 'vi',
    theme ENUM('LIGHT', 'DARK', 'SYSTEM') NOT NULL DEFAULT 'SYSTEM',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id),
    CONSTRAINT fk_settings_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_settings_app FOREIGN KEY (app_language) REFERENCES languages (code),
    CONSTRAINT fk_settings_trans FOREIGN KEY (translation_language) REFERENCES languages (code)
) ENGINE = InnoDB;

CREATE TABLE auth_sessions (
    id CHAR(36) CHARACTER SET ascii NOT NULL COMMENT 'Mã định danh phiên đăng nhập (UUID)',
    user_id BIGINT UNSIGNED NOT NULL COMMENT 'Người dùng sở hữu phiên đăng nhập',
    refresh_token_hash CHAR(64) CHARACTER SET ascii NOT NULL COMMENT 'Hash SHA-256 của refresh token hiện tại',
    prev_refresh_token_hash CHAR(64) CHARACTER SET ascii NULL COMMENT 'Hash refresh token trước đó để phát hiện tái sử dụng',
    device_id CHAR(36) CHARACTER SET ascii NULL COMMENT 'Mã định danh thiết bị đăng nhập',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    last_used_at DATETIME(3) NULL,
    expires_at DATETIME(3) NOT NULL COMMENT 'Thời điểm phiên hết hạn, tối đa 30 ngày kể từ khi tạo',
    revoked_at DATETIME(3) NULL COMMENT 'Thời điểm phiên bị thu hồi',
    PRIMARY KEY (id),
    UNIQUE KEY uq_session_refresh (refresh_token_hash),
    KEY idx_session_prev (prev_refresh_token_hash),
    KEY idx_session_user (user_id, revoked_at),
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE user_devices (
    device_id CHAR(36) CHARACTER SET ascii NOT NULL COMMENT 'UUID do Android sinh',
    user_id BIGINT UNSIGNED NOT NULL,
    session_id CHAR(36) CHARACTER SET ascii NOT NULL,
    platform VARCHAR(20) CHARACTER SET ascii NOT NULL,
    fcm_token VARCHAR(512) CHARACTER SET ascii NOT NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (device_id),
    KEY idx_device_user (user_id),
    KEY idx_device_session (session_id),
    CONSTRAINT fk_device_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_device_session FOREIGN KEY (session_id) REFERENCES auth_sessions (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE otp_challenges (
    id CHAR(36) CHARACTER SET ascii NOT NULL COMMENT 'challengeId',
    user_id BIGINT UNSIGNED NOT NULL,
    purpose ENUM('REGISTER', 'RESET_PASSWORD') NOT NULL,
    code_hash CHAR(64) CHARACTER SET ascii NOT NULL,
    status ENUM('ACTIVE', 'CONSUMED', 'INVALIDATED', 'LOCKED') NOT NULL DEFAULT 'ACTIVE' COMMENT 'LOCKED: nhập sai quá 5 lần',
    attempts TINYINT UNSIGNED NOT NULL DEFAULT 0,
    send_count SMALLINT UNSIGNED NOT NULL DEFAULT 1,
    last_sent_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    expires_at DATETIME(3) NOT NULL COMMENT 'Hiệu lực 5 phút',
    consumed_at DATETIME(3) NULL,
    reset_token_hash CHAR(64) CHARACTER SET ascii NULL,
    reset_token_expires_at DATETIME(3) NULL COMMENT 'Hiệu lực 10 phút',
    reset_token_used_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uq_otp_reset_token (reset_token_hash),
    KEY idx_otp_user (user_id, purpose, status),
    KEY idx_otp_user_created (user_id, created_at),
    CONSTRAINT fk_otp_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE files (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    owner_id BIGINT UNSIGNED NOT NULL,
    kind ENUM('IMAGE', 'VIDEO', 'FILE', 'VOICE') NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(127) CHARACTER SET ascii NOT NULL,
    size_bytes BIGINT UNSIGNED NOT NULL,
    duration_ms INT UNSIGNED NULL,
    width INT UNSIGNED NULL,
    height INT UNSIGNED NULL,
    storage_key VARCHAR(512) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    thumbnail_key VARCHAR(512) CHARACTER SET ascii COLLATE ascii_bin NULL,
    status ENUM('PENDING', 'ATTACHED') NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING: Chưa được liên kết, tự động xóa sau 24 giờ; ATTACHED: Đã được liên kết',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    attached_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_file_storage (storage_key),
    UNIQUE KEY uq_file_id_kind (id, kind),
    KEY idx_file_owner_status (owner_id, status, created_at),
    KEY idx_file_status_created (status, created_at),
    CONSTRAINT fk_file_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT ck_file_size CHECK (size_bytes > 0),
    CONSTRAINT ck_file_dim CHECK (
        kind IN ('IMAGE', 'VIDEO')
        OR (
            width IS NULL
            AND height IS NULL
        )
    ),
    CONSTRAINT ck_file_duration CHECK (
        kind IN ('VIDEO', 'VOICE')
        OR duration_ms IS NULL
    ),
    CONSTRAINT ck_file_voice CHECK (
        kind <> 'VOICE'
        OR (
            duration_ms IS NOT NULL
            AND duration_ms <= 300000
        )
    ),
    CONSTRAINT ck_file_attached CHECK (
        (status = 'ATTACHED') = (attached_at IS NOT NULL)
    )
) ENGINE = InnoDB;

ALTER TABLE
    users
ADD
    CONSTRAINT fk_user_avatar FOREIGN KEY (avatar_file_id) REFERENCES files (id) ON DELETE
SET
    NULL;

CREATE TABLE friend_requests (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    sender_id BIGINT UNSIGNED NOT NULL,
    receiver_id BIGINT UNSIGNED NOT NULL,
    status ENUM('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    responded_at DATETIME(3) NULL,
    pending_pair VARCHAR(41) CHARACTER SET ascii GENERATED ALWAYS AS (
        IF(
            status = 'PENDING',
            CONCAT(
                LEAST(sender_id, receiver_id),
                '-',
                GREATEST(sender_id, receiver_id)
            ),
            NULL
        )
    ) STORED COMMENT 'Duy nhất theo cặp khi đang chờ: chặn hai người gửi đồng thời',
    PRIMARY KEY (id),
    UNIQUE KEY uq_friend_req_pending (pending_pair),
    KEY idx_friend_req_receiver (receiver_id, status, id),
    KEY idx_friend_req_sender (sender_id, status, id),
    CONSTRAINT fk_freq_sender FOREIGN KEY (sender_id) REFERENCES users (id),
    CONSTRAINT fk_freq_receiver FOREIGN KEY (receiver_id) REFERENCES users (id),
    CONSTRAINT ck_freq_self CHECK (sender_id <> receiver_id)
) ENGINE = InnoDB;

CREATE TABLE friendships (
    user_low BIGINT UNSIGNED NOT NULL,
    user_high BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_low, user_high),
    KEY idx_friend_high (user_high, user_low),
    CONSTRAINT fk_friend_low FOREIGN KEY (user_low) REFERENCES users (id),
    CONSTRAINT fk_friend_high FOREIGN KEY (user_high) REFERENCES users (id),
    CONSTRAINT ck_friend_order CHECK (user_low < user_high)
) ENGINE = InnoDB;

CREATE TABLE conversations (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    type ENUM('DIRECT', 'GROUP') NOT NULL,
    title VARCHAR(150),
    created_by BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    FOREIGN KEY (created_by) REFERENCES users(id),
    CHECK (
        type <> 'GROUP'
        OR (
            title IS NOT NULL
            AND CHAR_LENGTH(TRIM(title)) > 0
        )
    )
) ENGINE = InnoDB;

CREATE TABLE conversations (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    type ENUM('DIRECT', 'GROUP') NOT NULL,
    title VARCHAR(100) NULL,
    avatar_file_id BIGINT UNSIGNED NULL,
    status ENUM('ACTIVE', 'CLOSED') NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT UNSIGNED NOT NULL,
    last_message_id BIGINT UNSIGNED NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_conv_updated (updated_at, id),
    CONSTRAINT fk_conv_creator FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_conv_avatar FOREIGN KEY (avatar_file_id) REFERENCES files (id) ON DELETE
    SET
        NULL,
        CONSTRAINT ck_conv_title CHECK (
            type = 'DIRECT'
            OR (
                title IS NOT NULL
                AND CHAR_LENGTH(TRIM(title)) >= 1
            )
        ),
        CONSTRAINT ck_conv_status CHECK (
            type = 'GROUP'
            OR status = 'ACTIVE'
        )
) ENGINE = InnoDB;

CREATE TABLE conversation_members (
    conversation_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    role ENUM('OWNER', 'ADMIN', 'MEMBER') NOT NULL DEFAULT 'MEMBER',
    joined_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    left_at DATETIME(3) NULL,
    left_reason ENUM('LEFT', 'REMOVED') NULL,
    last_read_message_id BIGINT UNSIGNED NULL COMMENT 'Mốc đọc, chỉ tiến lên',
    unread_count INT UNSIGNED NOT NULL DEFAULT 0,
    active_owner_of BIGINT UNSIGNED GENERATED ALWAYS AS (
        IF(
            role = 'OWNER'
            AND left_at IS NULL,
            conversation_id,
            NULL
        )
    ) STORED COMMENT 'Mỗi nhóm tối đa một OWNER đang hoạt động',
    PRIMARY KEY (conversation_id, user_id),
    UNIQUE KEY uq_one_owner (active_owner_of),
    KEY idx_member_user (user_id, left_at, conversation_id),
    CONSTRAINT fk_member_conv FOREIGN KEY (conversation_id) REFERENCES conversations (id),
    CONSTRAINT fk_member_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_member_left CHECK (
        (
            left_at IS NULL
            AND left_reason IS NULL
        )
        OR (
            left_at IS NOT NULL
            AND left_reason IS NOT NULL
            AND left_at >= joined_at
        )
    )
) ENGINE = InnoDB;

CREATE TABLE direct_conversations (
    conversation_id BIGINT UNSIGNED NOT NULL,
    user_low BIGINT UNSIGNED NOT NULL,
    user_high BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (conversation_id),
    UNIQUE KEY uq_direct_pair (user_low, user_high),
    CONSTRAINT fk_direct_low FOREIGN KEY (conversation_id, user_low) REFERENCES conversation_members (conversation_id, user_id),
    CONSTRAINT fk_direct_high FOREIGN KEY (conversation_id, user_high) REFERENCES conversation_members (conversation_id, user_id),
    CONSTRAINT ck_direct_order CHECK (user_low < user_high)
) ENGINE = InnoDB;

CREATE TABLE group_invite_links (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'inviteId',
    conversation_id BIGINT UNSIGNED NOT NULL,
    token_hash CHAR(64) CHARACTER SET ascii NOT NULL COMMENT 'inviteToken chỉ hiện một lần, lưu dạng băm',
    created_by BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    expires_at DATETIME(3) NOT NULL COMMENT '1-168 giờ, mặc định 24',
    revoked_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_invite_token (token_hash),
    KEY idx_invite_conv (conversation_id, revoked_at, expires_at),
    CONSTRAINT fk_invite_conv FOREIGN KEY (conversation_id) REFERENCES conversations (id),
    CONSTRAINT fk_invite_creator FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT ck_invite_expiry CHECK (expires_at > created_at)
) ENGINE = InnoDB;

NGINE = InnoDB;

CREATE TABLE messages (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT UNSIGNED NOT NULL,
    sender_id BIGINT UNSIGNED NOT NULL,
    client_message_id CHAR(36) CHARACTER SET ascii NOT NULL,
    type ENUM('TEXT', 'IMAGE', 'VIDEO', 'FILE', 'VOICE') NOT NULL DEFAULT 'TEXT',
    original_text TEXT NULL,
    reply_to_id BIGINT UNSIGNED NULL,
    forwarded_from_message_id BIGINT UNSIGNED NULL COMMENT 'Nội bộ, không trả ra API',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uq_message_dedup (sender_id, client_message_id),
    UNIQUE KEY uq_message_conv_id (conversation_id, id),
    FULLTEXT KEY ft_message_text (original_text) WITH PARSER ngram,
    KEY idx_message_forward (forwarded_from_message_id),
    CONSTRAINT fk_message_sender FOREIGN KEY (conversation_id, sender_id) REFERENCES conversation_members (conversation_id, user_id),
    CONSTRAINT fk_message_reply FOREIGN KEY (conversation_id, reply_to_id) REFERENCES messages (conversation_id, id),
    CONSTRAINT fk_message_forward FOREIGN KEY (forwarded_from_message_id) REFERENCES messages (id),
    CONSTRAINT ck_message_len CHECK (
        original_text IS NULL
        OR CHAR_LENGTH(original_text) <= 4000
    ),
    CONSTRAINT ck_message_text CHECK (
        type <> 'TEXT'
        OR (
            original_text IS NOT NULL
            AND CHAR_LENGTH(TRIM(original_text)) >= 1
        )
    ),
    CONSTRAINT ck_message_voice CHECK (
        type <> 'VOICE'
        OR original_text IS NULL
    )
) ENGINE = InnoDB;

ALTER TABLE
    conversations
ADD
    CONSTRAINT fk_conv_last_message FOREIGN KEY (id, last_message_id) REFERENCES messages (conversation_id, id);

ALTER TABLE
    conversation_members
ADD
    CONSTRAINT fk_member_last_read FOREIGN KEY (conversation_id, last_read_message_id) REFERENCES messages (conversation_id, id);

CREATE TABLE message_attachments (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT UNSIGNED NOT NULL COMMENT 'Phi chuẩn hóa để xem kho tệp theo hội thoại',
    message_id BIGINT UNSIGNED NOT NULL,
    file_id BIGINT UNSIGNED NOT NULL,
    kind ENUM('IMAGE', 'VIDEO', 'FILE', 'VOICE') NOT NULL COMMENT 'Bằng files.kind (ép bằng khóa ngoại kép fk_attachment_file)',
    position TINYINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uq_attachment_msg_file (message_id, file_id),
    KEY idx_attachment_file (file_id),
    KEY idx_attachment_gallery (conversation_id, kind, message_id),
    CONSTRAINT fk_attachment_message FOREIGN KEY (conversation_id, message_id) REFERENCES messages (conversation_id, id),
    CONSTRAINT fk_attachment_file FOREIGN KEY (file_id, kind) REFERENCES files (id, kind)
) ENGINE = InnoDB;

CREATE TABLE message_reactions (
    message_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    conversation_id BIGINT UNSIGNED NOT NULL,
    emoji VARCHAR(32) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (message_id, user_id),
    KEY idx_reaction_summary (message_id, emoji),
    CONSTRAINT fk_reaction_message FOREIGN KEY (conversation_id, message_id) REFERENCES messages (conversation_id, id),
    CONSTRAINT fk_reaction_member FOREIGN KEY (conversation_id, user_id) REFERENCES conversation_members (conversation_id, user_id)
) ENGINE = InnoDB;

CREATE TABLE forward_requests (
    sender_id BIGINT UNSIGNED NOT NULL,
    client_request_id CHAR(36) CHARACTER SET ascii NOT NULL,
    request_hash CHAR(64) CHARACTER SET ascii NOT NULL COMMENT 'Hash của tin nguồn + danh sách đích',
    result_json JSON NOT NULL COMMENT 'Các tin đã tạo, trả lại khi gửi lặp',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (sender_id, client_request_id),
    CONSTRAINT fk_forward_sender FOREIGN KEY (sender_id) REFERENCES users (id)
) ENGINE = InnoDB;

CREATE TABLE message_translations (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    message_id BIGINT UNSIGNED NOT NULL,
    target_language VARCHAR(35) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    source_language VARCHAR(35) CHARACTER SET ascii COLLATE ascii_bin NULL,
    translated_text TEXT NOT NULL,
    provider VARCHAR(80) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uq_translation (message_id, target_language),
    CONSTRAINT fk_translation_message FOREIGN KEY (message_id) REFERENCES messages (id),
    CONSTRAINT fk_translation_target FOREIGN KEY (target_language) REFERENCES languages (code),
    CONSTRAINT ck_translation_text CHECK (CHAR_LENGTH(TRIM(translated_text)) >= 1)
) ENGINE = InnoDB;

CREATE TABLE calls (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'callId',
    caller_id BIGINT UNSIGNED NOT NULL,
    callee_id BIGINT UNSIGNED NOT NULL,
    client_call_id CHAR(36) CHARACTER SET ascii NOT NULL,
    status ENUM(
        'RINGING',
        'ACCEPTED',
        'CONNECTED',
        'REJECTED',
        'CANCELLED',
        'MISSED',
        'FAILED',
        'ENDED'
    ) NOT NULL DEFAULT 'RINGING',
    caller_session_id CHAR(36) CHARACTER SET ascii NULL COMMENT 'Phiên người gọi khởi tạo',
    accepted_session_id CHAR(36) CHARACTER SET ascii NULL COMMENT 'Phiên người nhận thắng khi accept',
    caller_media_state ENUM('PENDING', 'CONNECTED', 'FAILED') NOT NULL DEFAULT 'PENDING',
    callee_media_state ENUM('PENDING', 'CONNECTED', 'FAILED') NOT NULL DEFAULT 'PENDING',
    end_reason ENUM(
        'HANGUP',
        'TIMEOUT',
        'DISCONNECTED',
        'CONNECTION_FAILED'
    ) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    ring_expires_at DATETIME(3) NOT NULL COMMENT 'created_at + 30 giây',
    accepted_at DATETIME(3) NULL,
    setup_deadline_at DATETIME(3) NULL COMMENT 'accepted_at + 30 giây',
    connected_at DATETIME(3) NULL,
    control_lost_at DATETIME(3) NULL COMMENT 'Mốc mất WebSocket, quá 30 giây thì ENDED/DISCONNECTED',
    ended_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_call_dedup (caller_id, client_call_id),
    KEY idx_call_caller (caller_id, status),
    KEY idx_call_callee (callee_id, status),
    KEY idx_call_ring (status, ring_expires_at),
    KEY idx_call_setup (status, setup_deadline_at),
    KEY idx_call_lost (status, control_lost_at),
    CONSTRAINT fk_call_caller FOREIGN KEY (caller_id) REFERENCES users (id),
    CONSTRAINT fk_call_callee FOREIGN KEY (callee_id) REFERENCES users (id),
    CONSTRAINT fk_call_accepted FOREIGN KEY (accepted_session_id) REFERENCES auth_sessions (id) ON DELETE
    SET
        NULL,
        CONSTRAINT ck_call_self CHECK (caller_id <> callee_id)
) ENGINE = InnoDB;

CREATE TABLE active_calls (
    user_id BIGINT UNSIGNED NOT NULL,
    call_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (user_id),
    KEY idx_active_call (call_id),
    CONSTRAINT fk_active_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_active_call FOREIGN KEY (call_id) REFERENCES calls (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TRIGGER trg_call_release_active
AFTER
UPDATE
    ON calls FOR EACH ROW
DELETE FROM
    active_calls
WHERE
    call_id = NEW.id
    AND NEW.ended_at IS NOT NULL;

SET
    FOREIGN_KEY_CHECKS = 1;