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