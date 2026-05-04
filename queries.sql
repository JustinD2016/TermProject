-- Purpose: Query selects the count of all people that a user follows. 
--URL: http://localhost:8080/profile
final String followingCount = "SELECT COUNT(*) FROM follow WHERE follower_id = ?"

-- Purpose: Query selects the count of all the people that follow the logged in user. 
-- URL: http://localhost:8080/profile

final String followerCount = "SELECT COUNT(*) FROM follow WHERE followee_id = ?"

-- Purpose: Query joins daily_game and game_id to display if logged in user has played the daily game and how many guesses
-- it took for the user to complete. 
-- URL: http://localhost:8080/profile

final String todayResult = "SELECT gs.guesses_used AS guesses_used, gs.solved AS solved FROM game_session gs " +
                                "JOIN daily_game dg ON gs.game_id = dg.game_id " + 
                                "WHERE gs.user_id = ? AND dg.game_date = CURDATE()";

-- Purpose: Query collects the daily game stats for all of the people that the logged in user follows. Results of this are displayed
-- nicely on the profile page. 
-- URL: http://localhost:8080/profile

final String followingStatusStatement = "SELECT u.username, gs.solved, gs.guesses_used " + 
            "FROM follow f JOIN user u ON f.followee_id = user_id " + 
            "LEFT JOIN game_session gs ON gs.user_id = u.user_id " + 
            "LEFT JOIN daily_game dg ON gs.game_id = dg.game_id AND dg.game_date = CURDATE() " + 
            "WHERE f.follower_id = ?";

-- Purpose: Query selects the logged in users current and max streak to display on the profile page. 
-- URL: http://localhost:8080/profile

final String streak = "SELECT current_streak, max_streak FROM user_stats WHERE user_id = ?";

-- Purpose: Query populates the top 100 global users statistics from the daily game and displays the username,
-- games played, games won, win rate, current streak, max_streak, avg_guesses, and user_rank in ascending order. 
-- URL: http://localhost:8080/standings

final String allUsersSql =
            "SELECT u.username, " +
            "us.games_played, us.games_won, " +
            "ROUND(us.games_won * 100.0 / NULLIF(us.games_played, 0), 1) AS win_rate, " +
            "us.current_streak, us.max_streak, us.avg_guesses, " +
            "RANK() OVER (ORDER BY us.games_won DESC, us.avg_guesses ASC) AS user_rank " +
            "FROM user_stats us JOIN user u ON us.user_id = u.user_id " +
            "ORDER BY user_rank ASC LIMIT 100";

-- Purpose: Query populates the users statistics of the users the logged in user follows for the daily game and displays the username,
-- games played, games won, win rate, current streak, max_streak, avg_guesses, and user_rank in ascending order. 
-- URL: http://localhost:8080/standings

final String friendsSql =
            "SELECT u.username, " +
            "us.games_played, us.games_won, " +
            "ROUND(us.games_won * 100.0 / NULLIF(us.games_played, 0), 1) AS win_rate, " +
            "us.current_streak, us.max_streak, us.avg_guesses, " +
            "RANK() OVER (ORDER BY us.games_won DESC, us.avg_guesses ASC) AS user_rank " +
            "FROM user_stats us JOIN user u ON us.user_id = u.user_id " +
            "WHERE us.user_id = ? OR us.user_id IN " +
            "(SELECT followee_id FROM follow WHERE follower_id = ?) " +
            "ORDER BY user_rank ASC";

-- Purpose: Query pulls logged in user's statistics to populate the pie chart on the standings page. 
-- URL: http://localhost:8080/standings

final String myStatsSql =
            "SELECT games_won, (games_played - games_won) AS games_lost " +
            "FROM user_stats WHERE user_id = ?";

-- Purpose: Query selects all of the information for a specific actor based on the actor_id. This is
-- used to populate an actor object that can be loaded in during a game.
-- URL: http://localhost:8080/
-- URL: http://localhost:8080/feed

final String actorSql =
            "SELECT actor_id, first_name, last_name, primary_profession, " +
            "       birth_year, death_year " +
            "FROM actor WHERE actor_id = ?";

-- Purpose: Query joins actor_title and title to get all the title information for which an actor has been in.
-- URL: http://localhost:8080/
-- URL: http://localhost:8080/feed

final String titleSql =
            "SELECT t.title_id, t.primary_title, t.title_type, t.start_year, t.genres " +
            "FROM actor_title at " +
            "JOIN title t ON t.title_id = at.title_id " +
            "WHERE at.actor_id = ?";

-- Purpose: Query selects the game_id and actor_id for the daily game by matching on the current date.
-- URL: http://localhost:8080/

final String todaysGameSql =
            "SELECT game_id, actor_id FROM daily_game WHERE game_date = CURDATE()";

-- Purpose: Query selects only the actor id so its easier to retrieve the actor once the game is over.
-- URL: http://localhost:8080/

final String todaysActorSql =
            "SELECT actor_id FROM daily_game WHERE game_date = CURDATE()";

-- Purpose: Query gets the logged in user's statistics for today's daily game.
-- URL: http://localhost:8080/

final String getSessionSql =
            "SELECT session_id, guesses_used, solved FROM game_session " +
            "WHERE user_id = ? AND game_id = ?";

-- Purpose: Query creates a new game seassion for the logged in user on their first visit of the game page for the day.
-- It starts with 0 guessed used and 0 solved since no guesses have been made yet (the game has not been solved yet).
-- URL: http://localhost:8080/

final String insertSql =
                "INSERT INTO game_session (user_id, game_id, guesses_used, solved) " +
                "VALUES (?, ?, 0, 0)";

-- Purpose: Query retrieves all guesses in a given game, ordered by the guess number. For each guess number row it
-- returns the guessed actor id and the hint result for displaying the JSON in the feed recreation of the game.
-- URL: http://localhost:8080/
-- URL: http://localhost:8080/feed

final String loadGuessesSql =
            "SELECT guess_number, guessed_actor_id, hint_result " +
            "FROM guess " +
            "WHERE session_id = ? " +
            "ORDER BY guess_number ASC";

-- Purpose: Query inserts a new row into the guess table each time the user submits a guess. Stores which session
-- the game is for, which actor was guessed, the guess number, and the JSON hint result.
-- URL http://localhost:8080/guess

final String insertGuessSql =
            "INSERT INTO guess (session_id, guessed_actor_id, guess_number, hint_result) " +
            "VALUES (?, ?, ?, ?)";

-- Purpose: Query updates the game session after each guess to keep track of guesses used and whether the game has been solved.
-- URL: http://localhost:8080/guess

final String updateSessionSql =
            "UPDATE game_session SET guesses_used = ?, solved = ? WHERE session_id = ?";

-- Purpose: Query retrieves the user's current stats so that they can be updated based on the result of the current game.
-- URL: http://localhost:8080/

final String getStatsSql =
            "SELECT games_played, games_won, current_streak, max_streak, avg_guesses " +
            "FROM user_stats WHERE user_id = ?";

-- Purpose: Query updates the user's stats after the game is over based on the result of the game.
-- URL: http://localhost:8080/

final String updateSql =
                        "UPDATE user_stats " +
                        "SET games_played = ?, games_won = ?, current_streak = ?, " +
                        "    max_streak = ?, avg_guesses = ? " +
                        "WHERE user_id = ?";

-- Purpose: Query generates the daily game by picking a random actor for the current date. 
-- Runs automatically every night at mighnight EST via the @Scheduled(cron = "0 0 0 * * ?") annotation.

final String generateDailySql = "INSERT into daily_game (game_date, actor_id) " +
                           "SELECT " + 
                           "CURDATE(), " + 
                           "a.actor_id " +
                           "FROM actor a " +
                           "ORDER BY RAND() LIMIT 1";

-- Purpose: Query looks up an actor by their first and last name when a gues is submitted.
-- Returns the actor id for the guessed actor, so that it can load the full actor details for the hint.
-- URL: http://localhost:8080/

final String findActorSql =
                "SELECT actor_id FROM actor WHERE first_name = ? AND last_name = ? LIMIT 1";

-- Purpose: Query retrieves all completed games for logged in user and everyone they follow to populate the feed page.
-- URL: http://localhost:8080/feed

final String feedSql = "SELECT " +
            "g.session_id, " +
            "g.guesses_used, " +
            "g.solved, " +
            "DATE_FORMAT(CONVERT_TZ(g.played_at, '+00:00', '-04:00'), '%b %d, %Y, %h:%i %p') AS played_at, " +
            "u.username, " +
            "u.user_id, " +
            "a.first_name, " +
            "a.last_name " +
            "FROM game_session g " +
            "JOIN user u ON g.user_id = u.user_id " +
            "JOIN daily_game dg ON g.game_id = dg.game_id " +
            "JOIN actor a ON dg.actor_id = a.actor_id " +
            "WHERE (g.solved = 1 OR g.guesses_used >= 6) " +
            "AND (g.user_id IN (SELECT followee_id FROM follow WHERE follower_id = ?) OR g.user_id = ?) " +
            "ORDER BY g.played_at DESC";

-- Purpose: Query retrieves all users that the logged in user is not already following, so that they can be displayed
-- on the people page as an option to be followed. It also retrieves their last activity in EST time zone format.
-- URL: http://localhost:8080/people

final String followableUsersSql =
            "SELECT u.user_id, u.username, " +
            "       COALESCE(DATE_FORMAT(CONVERT_TZ(gs.last_played, '+00:00', '-04:00'), 'Last played on %b %d, %Y'), 'No games played yet') AS lastActivity " +
            "FROM user u " +
            "LEFT JOIN ( " +
            "    SELECT user_id, MAX(played_at) AS last_played " +
            "    FROM game_session " +
            "    GROUP BY user_id " +
            ") gs ON gs.user_id = u.user_id " +
            "WHERE u.user_id != ? " +
            "  AND u.user_id NOT IN ( " +
            "      SELECT followee_id FROM follow WHERE follower_id = ? " +
            "  )";

-- Purpose: This query was for testing purposes (not used), but it retrieves all the users that the logged in user is following.
-- URL: http://localhost:8080/profile

final String followingSql =
            "SELECT u.user_id, u.username, " +
            "       gs.solved, gs.guesses_used, " +
            "       COALESCE(DATE_FORMAT(CONVERT_TZ(gs.last_played, '+00:00', '-04:00'), '%b %d, %Y, %h:%i %p'), 'Not played today') AS lastActivity " +
            "FROM follow f " +
            "≈ " +
            "LEFT JOIN game_session gs ON gs.user_id = u.user_id " +
            "    AND gs.game_id = (SELECT game_id FROM daily_game WHERE game_date = CURDATE()) " +
            "WHERE f.follower_id = ?";

-- Purpose: This query was for testing purposes (not used), but it inserts a new row into the follow table to signiy a user now follows another user.
-- URL: http://localhost:8080/people

final String followSql =
            "INSERT IGNORE INTO follow (follower_id, followee_id) VALUES (?, ?)";

-- Purpose: Query deletes a row from the follow table where the user's id matches the follower's id
-- URL: http://localhost:8080/people

final String unfollowSql =
            "DELETE FROM follow WHERE follower_id = ? AND followee_id = ?";

-- Purpose: This query was for testing purposes (not used), but it checks if a row exists in the follow table,
-- to see if a user passed in follows anotother user passed in.
-- URL: http://localhost:8080/people

final String isFollowingSql =
            "SELECT 1 FROM follow WHERE follower_id = ? AND followee_id = ? LIMIT 1";

-- Purpose: Query looks up a user_id and retrieves their stored password hash, the hash will then be compared
-- against the hash of the password that was entered to see if they can be logged in.
-- URL: http://localhost:8080/login

final String authenticateSql = "SELECT user_id, username, password_hash FROM user WHERE username = ?";

-- Purpose: Query inserts a new user and their account information into the user table after regiserting.
-- URL: http://localhost:8080/register

final String registerSql =
            "INSERT INTO user (username, email, password_hash, salt) VALUES (?, ?, ?, ?)";

-- Purpose: Query inserts an empty user stats row into the user stats table so the new user can have their stats be tracked.
-- URL: http://localhost:8080/register

final String statsSql = "INSERT INTO user_stats (user_id) VALUES (?)";