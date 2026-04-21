-- ============================================================
--  ActorDle - IMDb Actor Guessing Game
--  Database Setup Script (MySQL)
-- ============================================================
--  Entity sets: USER, USER_STATS, FOLLOW, ACTOR, TITLE,
--               ACTOR_TITLE, DAILY_GAME, GAME_SESSION, GUESS
-- ============================================================



-- ============================================================
-- 1. USER
-- ============================================================
CREATE TABLE IF NOT EXISTS user (
    user_id       INT           NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)   NOT NULL,
    email         VARCHAR(255)  NOT NULL,
    password_hash VARCHAR(255)  NOT NULL,
    salt          VARCHAR(255)  NOT NULL,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id),
    UNIQUE KEY uq_user_username (username),
    UNIQUE KEY uq_user_email    (email)
);

-- ============================================================
-- 2. USER_STATS
-- ============================================================
CREATE TABLE IF NOT EXISTS user_stats (
    stat_id        INT          NOT NULL AUTO_INCREMENT,
    user_id        INT          NOT NULL,
    games_played   INT          NOT NULL DEFAULT 0,
    games_won      INT          NOT NULL DEFAULT 0,
    current_streak INT          NOT NULL DEFAULT 0,
    max_streak     INT          NOT NULL DEFAULT 0,
    avg_guesses    DECIMAL(5,2) NOT NULL DEFAULT 0.00,

    PRIMARY KEY (stat_id),
    UNIQUE KEY uq_stats_user (user_id),
    CONSTRAINT fk_stats_user
        FOREIGN KEY (user_id) REFERENCES user (user_id)
        ON DELETE CASCADE
);

-- ============================================================
-- 3. FOLLOW  (Twitter-style, one-way)
-- ============================================================
CREATE TABLE IF NOT EXISTS follow (
    follower_id INT      NOT NULL,
    followee_id INT      NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (follower_id, followee_id),
    CONSTRAINT fk_follow_follower
        FOREIGN KEY (follower_id) REFERENCES user (user_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_follow_followee
        FOREIGN KEY (followee_id) REFERENCES user (user_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_no_self_follow
        CHECK (follower_id <> followee_id)
);

CREATE INDEX idx_follow_followee ON follow (followee_id);

-- ============================================================
-- 4. ACTOR
-- ============================================================
CREATE TABLE IF NOT EXISTS actor (
    actor_id           VARCHAR(20)  NOT NULL,
    first_name         VARCHAR(100) NOT NULL,
    last_name          VARCHAR(100) NOT NULL,
    birth_year         SMALLINT,
    death_year         SMALLINT,
    primary_profession VARCHAR(100),

    PRIMARY KEY (actor_id)
);

CREATE INDEX idx_actor_last_name  ON actor (last_name);
CREATE INDEX idx_actor_birth_year ON actor (birth_year);

-- ============================================================
-- 5. TITLE
-- ============================================================
CREATE TABLE IF NOT EXISTS title (
    title_id        VARCHAR(20)  NOT NULL,
    title_type      VARCHAR(50),
    primary_title   VARCHAR(500) NOT NULL,
    original_title  VARCHAR(500),
    is_adult        TINYINT(1)   NOT NULL DEFAULT 0,
    start_year      SMALLINT,
    end_year        SMALLINT,
    runtime_minutes SMALLINT,
    genres          VARCHAR(255),

    PRIMARY KEY (title_id)
);

CREATE INDEX idx_title_primary ON title (primary_title(100));

-- ============================================================
-- 6. ACTOR_TITLE  (junction table)
-- ============================================================
CREATE TABLE IF NOT EXISTS actor_title (
    actor_id VARCHAR(20) NOT NULL,
    title_id VARCHAR(20) NOT NULL,

    PRIMARY KEY (actor_id, title_id),
    CONSTRAINT fk_at_actor
        FOREIGN KEY (actor_id) REFERENCES actor (actor_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_at_title
        FOREIGN KEY (title_id) REFERENCES title (title_id)
        ON DELETE CASCADE
);

CREATE INDEX idx_at_title ON actor_title (title_id);

-- ============================================================
-- 7. DAILY_GAME
-- ============================================================
CREATE TABLE IF NOT EXISTS daily_game (
    game_id   INT         NOT NULL AUTO_INCREMENT,
    game_date DATE        NOT NULL,
    actor_id  VARCHAR(20) NOT NULL,

    PRIMARY KEY (game_id),
    UNIQUE KEY uq_daily_game_date (game_date),
    CONSTRAINT fk_dg_actor
        FOREIGN KEY (actor_id) REFERENCES actor (actor_id)
);

-- ============================================================
-- 8. GAME_SESSION
-- ============================================================
CREATE TABLE IF NOT EXISTS game_session (
    session_id   INT        NOT NULL AUTO_INCREMENT,
    user_id      INT        NOT NULL,
    game_id      INT        NOT NULL,
    guesses_used TINYINT    NOT NULL DEFAULT 0,
    solved       TINYINT(1) NOT NULL DEFAULT 0,
    played_at    DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (session_id),
    UNIQUE KEY uq_session_user_game (user_id, game_id),
    CONSTRAINT fk_session_user
        FOREIGN KEY (user_id) REFERENCES user (user_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_session_game
        FOREIGN KEY (game_id) REFERENCES daily_game (game_id)
);

CREATE INDEX idx_session_game ON game_session (game_id);

-- ============================================================
-- 9. GUESS
-- ============================================================
CREATE TABLE IF NOT EXISTS guess (
    guess_id         INT         NOT NULL AUTO_INCREMENT,
    session_id       INT         NOT NULL,
    guessed_actor_id VARCHAR(20) NOT NULL,
    guess_number     TINYINT     NOT NULL,
    hint_result      JSON        NOT NULL,
    guessed_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (guess_id),
    CONSTRAINT fk_guess_session
        FOREIGN KEY (session_id) REFERENCES game_session (session_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_guess_actor
        FOREIGN KEY (guessed_actor_id) REFERENCES actor (actor_id),
    CONSTRAINT chk_guess_number
        CHECK (guess_number BETWEEN 1 AND 6)
);

CREATE INDEX idx_guess_session ON guess (session_id);