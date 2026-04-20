-- ============================================================
--  ActorDle - IMDb Actor Guessing Game
--  Database Setup Script (MySQL)
--  ============================================================
--  Entity sets: USER, ACTOR, TITLE, ACTOR_TITLE,
--               DAILY_GAME, GAME_SESSION, GUESS,
--               FRIEND, USER_STATS
-- ============================================================

CREATE DATABASE IF NOT EXISTS actordle;
USE actordle;

-- ============================================================
-- 1. USER
--    Stores registered players. Passwords are stored as a
--    bcrypt hash + salt (never plaintext).
-- ============================================================
CREATE TABLE IF NOT EXISTS user (
    user_id      INT            NOT NULL AUTO_INCREMENT,
    username     VARCHAR(50)    NOT NULL,
    email        VARCHAR(255)   NOT NULL,
    password_hash VARCHAR(255)  NOT NULL,  
    salt         VARCHAR(255)   NOT NULL,  
    created_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id),
    UNIQUE KEY uq_user_username (username),
    UNIQUE KEY uq_user_email    (email)
);

-- ============================================================
-- 2. USER_STATS
--    One row per user tracking cumulative game statistics.
--    Updated after every completed GAME_SESSION.
-- ============================================================
CREATE TABLE IF NOT EXISTS user_stats (
    stat_id        INT            NOT NULL AUTO_INCREMENT,
    user_id        INT            NOT NULL,
    games_played   INT            NOT NULL DEFAULT 0,
    games_won      INT            NOT NULL DEFAULT 0,
    current_streak INT            NOT NULL DEFAULT 0,
    max_streak     INT            NOT NULL DEFAULT 0,
    avg_guesses    DECIMAL(5,2)   NOT NULL DEFAULT 0.00,

    PRIMARY KEY (stat_id),
    UNIQUE KEY uq_stats_user (user_id),
    CONSTRAINT fk_stats_user
        FOREIGN KEY (user_id) REFERENCES user (user_id)
        ON DELETE CASCADE
);

-- ============================================================
-- 3. FRIEND
--    Mutual (Facebook-style) friendship using a status field.
--    status: 'pending' | 'accepted' | 'declined'
--    The pair (requester_id, addressee_id) is unique;
--    always store with requester_id < addressee_id to avoid
--    duplicate inverse rows, OR enforce at app level.
-- ============================================================
CREATE TABLE IF NOT EXISTS friend (
    requester_id INT          NOT NULL,
    addressee_id INT          NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'pending',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (requester_id, addressee_id),
    CONSTRAINT fk_friend_requester
        FOREIGN KEY (requester_id) REFERENCES user (user_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_friend_addressee
        FOREIGN KEY (addressee_id) REFERENCES user (user_id)
        ON DELETE CASCADE,
    CONSTRAINT chk_friend_status
        CHECK (status IN ('pending', 'accepted', 'declined')),
    CONSTRAINT chk_no_self_friend
        CHECK (requester_id <> addressee_id)
);

-- Index: quickly find all friendships involving a given user
CREATE INDEX idx_friend_addressee ON friend (addressee_id);

-- ============================================================
-- 4. ACTOR
--    Populated from the IMDB names.csv dataset.
--    actor_id = nconst from IMDB (e.g. "nm0000102").
--    first_name / last_name split from primaryName at import.
--    primary_profession stores the first listed profession.
-- ============================================================
CREATE TABLE IF NOT EXISTS actor (
    actor_id           VARCHAR(20)   NOT NULL,
    first_name         VARCHAR(100)  NOT NULL,
    last_name          VARCHAR(100)  NOT NULL,
    birth_year         SMALLINT,
    death_year         SMALLINT,
    primary_profession VARCHAR(100),

    PRIMARY KEY (actor_id)
);

-- Index: support game search page filters by name
CREATE INDEX idx_actor_last_name  ON actor (last_name);
CREATE INDEX idx_actor_birth_year ON actor (birth_year);

-- ============================================================
-- 5. TITLE
--    Populated from the IMDB titles.csv dataset.
--    title_id = tconst from IMDB (e.g. "tt0068646").
--    genres stored as comma-separated string matching IMDB format.
-- ============================================================
CREATE TABLE IF NOT EXISTS title (
    title_id         VARCHAR(20)   NOT NULL,
    title_type       VARCHAR(50),
    primary_title    VARCHAR(500)  NOT NULL,
    original_title   VARCHAR(500),
    is_adult         TINYINT(1)    NOT NULL DEFAULT 0,
    start_year       SMALLINT,
    end_year         SMALLINT,
    runtime_minutes  SMALLINT,
    genres           VARCHAR(255),

    PRIMARY KEY (title_id)
);

-- Index: support search/filter on the search database page
CREATE INDEX idx_title_primary ON title (primary_title(100));

-- ============================================================
-- 6. ACTOR_TITLE  (junction / associative entity)
--    Many-to-many between ACTOR and TITLE.
--    Derived from knownForTitles in names.csv and
--    the principals/crew files if available.
-- ============================================================
CREATE TABLE IF NOT EXISTS actor_title (
    actor_id  VARCHAR(20) NOT NULL,
    title_id  VARCHAR(20) NOT NULL,

    PRIMARY KEY (actor_id, title_id),
    CONSTRAINT fk_at_actor
        FOREIGN KEY (actor_id) REFERENCES actor (actor_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_at_title
        FOREIGN KEY (title_id) REFERENCES title (title_id)
        ON DELETE CASCADE
);

-- Index: look up all actors in a given title efficiently
CREATE INDEX idx_at_title ON actor_title (title_id);

-- ============================================================
-- 7. DAILY_GAME
--    One row per calendar day — the selected answer actor.
--    Enforced unique on game_date so there is exactly one
--    daily challenge per day.
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
--    One row per user per daily game (enforced by unique key).
--    guesses_used: 0–6, solved: true/false.
-- ============================================================
CREATE TABLE IF NOT EXISTS game_session (
    session_id   INT          NOT NULL AUTO_INCREMENT,
    user_id      INT          NOT NULL,
    game_id      INT          NOT NULL,
    guesses_used TINYINT      NOT NULL DEFAULT 0,
    solved       TINYINT(1)   NOT NULL DEFAULT 0,
    played_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (session_id),
    UNIQUE KEY uq_session_user_game (user_id, game_id),  -- one attempt per day
    CONSTRAINT fk_session_user
        FOREIGN KEY (user_id) REFERENCES user (user_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_session_game
        FOREIGN KEY (game_id) REFERENCES daily_game (game_id)
);

-- Index: leaderboard queries filter/sort by game_id
CREATE INDEX idx_session_game ON game_session (game_id);

-- ============================================================
-- 9. GUESS
--    Each individual guess within a session.
--    hint_result: JSON string encoding which attributes matched,
--    e.g. {"birth_year":"higher","profession":"match","titles":["tt0068646"]}
--    guess_number: 1–6 (max guesses per day).
-- ============================================================
CREATE TABLE IF NOT EXISTS guess (
    guess_id          INT           NOT NULL AUTO_INCREMENT,
    session_id        INT           NOT NULL,
    guessed_actor_id  VARCHAR(20)   NOT NULL,
    guess_number      TINYINT       NOT NULL,
    hint_result       JSON          NOT NULL,
    guessed_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (guess_id),
    CONSTRAINT fk_guess_session
        FOREIGN KEY (session_id) REFERENCES game_session (session_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_guess_actor
        FOREIGN KEY (guessed_actor_id) REFERENCES actor (actor_id),
    CONSTRAINT chk_guess_number
        CHECK (guess_number BETWEEN 1 AND 6)
);

-- Index: retrieve all guesses for a session quickly
CREATE INDEX idx_guess_session ON guess (session_id);


-- ============================================================
-- SAMPLE SEED DATA (small set — real data loaded via script)
-- ============================================================

-- Two test users (passwords are bcrypt hashes of "password123")
INSERT INTO user (username, email, password_hash, salt) VALUES
  ('testuser1', 'user1@example.com',
   '$2a$12$examplehashforuser1xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx',
   '$2a$12$exampleSaltForUser1xxxxx'),
  ('testuser2', 'user2@example.com',
   '$2a$12$examplehashforuser2xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx',
   '$2a$12$exampleSaltForUser2xxxxx');

-- Stat rows for each user
INSERT INTO user_stats (user_id) VALUES (1), (2);

-- Friendship between user 1 and user 2
INSERT INTO friend (requester_id, addressee_id, status) VALUES (1, 2, 'accepted');

-- Sample actors (small subset; bulk load from names.csv via import script)
INSERT INTO actor (actor_id, first_name, last_name, birth_year, death_year, primary_profession) VALUES
  ('nm0000102', 'Bryan',   'Cranston',  1956, NULL, 'actor'),
  ('nm0000288', 'Bryan',   'Cranston',  1956, NULL, 'actor'),
  ('nm0001674', 'Bryan',   'Cranston',  1956, NULL, 'actor'),
  ('nm0000151', 'Jack',    'Nicholson', 1937, NULL, 'actor'),
  ('nm0000093', 'Anthony', 'Hopkins',   1937, NULL, 'actor');

-- Sample titles
INSERT INTO title (title_id, title_type, primary_title, start_year, genres) VALUES
  ('tt0903747', 'tvSeries', 'Breaking Bad',          2008, 'Crime,Drama,Thriller'),
  ('tt0107290', 'movie',    'Jurassic Park',          1993, 'Action,Adventure,Sci-Fi'),
  ('tt0111161', 'movie',    'The Shawshank Redemption',1994,'Drama'),
  ('tt0068646', 'movie',    'The Godfather',           1972,'Crime,Drama');

-- Actor-title links
INSERT INTO actor_title (actor_id, title_id) VALUES
  ('nm0000102', 'tt0903747'),
  ('nm0000151', 'tt0068646');

-- Daily game entry for today
INSERT INTO daily_game (game_date, actor_id) VALUES
  (CURDATE(), 'nm0000102');


-- ============================================================
-- KEY QUERIES (referenced in the application)
-- ============================================================

-- Q1 [Game page] Fetch today's answer actor with their known titles
--    (JOIN — used by backend to generate hints)
-- SELECT a.actor_id, a.first_name, a.last_name, a.birth_year,
--        a.death_year, a.primary_profession,
--        GROUP_CONCAT(t.primary_title) AS known_titles,
--        GROUP_CONCAT(t.genres)        AS genres
-- FROM daily_game dg
-- JOIN actor a ON a.actor_id = dg.actor_id
-- JOIN actor_title at ON at.actor_id = a.actor_id
-- JOIN title t ON t.title_id = at.title_id
-- WHERE dg.game_date = CURDATE()
-- GROUP BY a.actor_id;

-- Q2 [Game page] Insert a new guess (prepared statement in Java)
-- INSERT INTO guess (session_id, guessed_actor_id, guess_number, hint_result)
-- VALUES (?, ?, ?, ?);

-- Q3 [Leaderboard] Daily scores with optional friend filter (aggregation + join)
-- SELECT u.username, gs.guesses_used, gs.solved, gs.played_at
-- FROM game_session gs
-- JOIN user u ON u.user_id = gs.user_id
-- JOIN daily_game dg ON dg.game_id = gs.game_id
-- WHERE dg.game_date = CURDATE()
--   AND gs.solved = 1
-- ORDER BY gs.guesses_used ASC, gs.played_at ASC;

-- Q4 [Statistics page] Per-user stats with rank (aggregation)
-- SELECT u.username, us.games_played, us.games_won,
--        us.current_streak, us.max_streak, us.avg_guesses,
--        RANK() OVER (ORDER BY us.games_won DESC) AS win_rank
-- FROM user_stats us
-- JOIN user u ON u.user_id = us.user_id;

-- Q5 [Search page] Filter actors by name / profession / birth year range
-- SELECT a.actor_id, a.first_name, a.last_name,
--        a.birth_year, a.primary_profession,
--        GROUP_CONCAT(t.primary_title) AS known_titles
-- FROM actor a
-- LEFT JOIN actor_title at ON at.actor_id = a.actor_id
-- LEFT JOIN title t ON t.title_id = at.title_id
-- WHERE a.last_name LIKE ?
--   AND a.birth_year BETWEEN ? AND ?
-- GROUP BY a.actor_id;

-- Q6 [Friend list] Get friends with their result on today's game (join + aggregation)
-- SELECT u.username,
--        gs.solved,
--        gs.guesses_used,
--        gs.played_at
-- FROM friend f
-- JOIN user u ON (
--     u.user_id = CASE WHEN f.requester_id = ? THEN f.addressee_id
--                      ELSE f.requester_id END
-- )
-- LEFT JOIN game_session gs ON gs.user_id = u.user_id
-- LEFT JOIN daily_game dg   ON dg.game_id = gs.game_id AND dg.game_date = CURDATE()
-- WHERE (f.requester_id = ? OR f.addressee_id = ?)
--   AND f.status = 'accepted';
