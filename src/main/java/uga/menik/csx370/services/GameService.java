package uga.menik.csx370.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;


import uga.menik.csx370.models.Actor;
import uga.menik.csx370.models.GameSession;
import uga.menik.csx370.models.Guess;
import uga.menik.csx370.models.Title;

/**
 * Service handling all game business logic.
 *
 * Responsibilities:
 *   - Loading Actor objects with their Title lists
 *   - Finding or creating a GameSession for today
 *   - Building hint result JSON by comparing two Actor objects
 *   - Inserting guess rows and updating game_session
 *   - Updating user_stats when a game ends
 *
 * GameController delegates all of this here and only
 * handles HTTP mapping and template rendering.
 */
@Service
public class GameService {

    private final DataSource dataSource;

    @Autowired
    public GameService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Loads an Actor by ID, including their list of Titles.
     * @param conn Database connection to use for the query
     * @param actorId ID of the actor to load
     * @return Actor object with all fields populated, or null if no actor found with that ID
     * @throws SQLException if a database access error occurs
     */
    public Actor loadActor(Connection conn, String actorId) throws SQLException {
        // Query for actor details
        final String actorSql =
            "SELECT actor_id, first_name, last_name, primary_profession, " +
            "       birth_year, death_year " +
            "FROM actor WHERE actor_id = ?";

        // Query for titles associated with this actor
        final String titleSql =
            "SELECT t.title_id, t.primary_title, t.title_type, t.start_year, t.genres " +
            "FROM actor_title at " +
            "JOIN title t ON t.title_id = at.title_id " +
            "WHERE at.actor_id = ?";

        // First pull the actor query 
        try (PreparedStatement pstmt = conn.prepareStatement(actorSql)) {
            //set the actor id to the prepared statement for the title query to pull all titles for that actor
            pstmt.setString(1, actorId);
            // Pull the titles associated with the actor and build Title objects for each
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    List<Title> titles = new ArrayList<>();

                    try (PreparedStatement titleStmt = conn.prepareStatement(titleSql)) {
                        titleStmt.setString(1, actorId);

                        try (ResultSet trs = titleStmt.executeQuery()) {
                            while (trs.next()) {
                                List<String> genres = trs.getString("genres") != null
                                    ? Arrays.asList(trs.getString("genres").split(","))
                                    : new ArrayList<>();

                                titles.add(new Title(
                                    trs.getString("title_id"),
                                    trs.getString("primary_title"),
                                    trs.getString("title_type"),
                                    trs.getInt("start_year"),
                                    genres
                                ));
                            }
                        }
                    }

                    return new Actor(
                        rs.getString("actor_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("primary_profession"),
                        rs.getInt("birth_year"),
                        rs.getInt("death_year"),
                        titles
                    );
                }
            }
        }
        return null;
    }

    /**
     * Find today's game_id and answer actor_id from daily_game.
     * @param conn Database connection to use for the query
     * @return int array where index 0 is game_id and index 1 is actor_id, or -1 if no game found for today
     * @throws SQLException if a database access error occurs
     */
    public int[] getTodaysGame(Connection conn) throws SQLException {
        // Query for today's game
        final String sql =
            "SELECT game_id, actor_id FROM daily_game WHERE game_date = CURDATE()";

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return new int[]{ rs.getInt("game_id") };
            }
        }
        return new int[]{ -1 };
    }

    /**
     * Find today's answer actor_id from daily_game.
     * @param conn Database connection to use for the query
     * @return String or null if no game found for today
     * @throws SQLException if a database access error occurs
     */
    public String getTodaysActorId(Connection conn) throws SQLException {
        final String sql =
            "SELECT actor_id FROM daily_game WHERE game_date = CURDATE()";

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getString("actor_id");
            }
        }
        return null;
    }

    /**
     * Find an existing GameSession for the user + game, or create one if it doesn't exist.
     * @param conn Database connection to use for the query
     * @param userId ID of the user playing the game
     * @param gameId ID of today's game
     * @return GameSession object with all fields populated, including a list of Guess objects with full Actor details
     * @throws SQLException
     */
    public GameSession getOrCreateSession(Connection conn, int userId, int gameId)
            throws SQLException {
        
        // First try to find an existing session for this user + game
        final String getSessionSql =
            "SELECT session_id, guesses_used, solved FROM game_session " +
            "WHERE user_id = ? AND game_id = ?";

        int sessionId = -1;
        int guessesUsed = 0;
        boolean solved = false;

        try (PreparedStatement pstmt = conn.prepareStatement(getSessionSql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, gameId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    sessionId   = rs.getInt("session_id");
                    guessesUsed = rs.getInt("guesses_used");
                    solved      = rs.getBoolean("solved");
                }
            }
        }

        // No session yet then we create one
        if (sessionId == -1) {
            final String insertSql =
                "INSERT INTO game_session (user_id, game_id, guesses_used, solved) " +
                "VALUES (?, ?, 0, 0)";

            try (PreparedStatement pstmt = conn.prepareStatement(
                    insertSql, PreparedStatement.RETURN_GENERATED_KEYS)) {

                pstmt.setInt(1, userId);
                pstmt.setInt(2, gameId);
                pstmt.executeUpdate();

                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        sessionId = keys.getInt(1);
                    }
                }
            }
        }

        // Load all guesses made so far in this session
        List<Guess> guesses = loadGuesses(conn, sessionId);

        return new GameSession(
            String.valueOf(sessionId),
            String.valueOf(userId),
            String.valueOf(gameId),
            guessesUsed,
            solved,
            guesses
        );
    }

    /**
     * Load all guesses for a given session, including the full Actor details for each guessed actor.
     * @param conn Database connection to use for the query
     * @param sessionId ID of the game session to load guesses for
     * @return List of Guess objects with all fields populated
     * @throws SQLException
     */
    public List<Guess> loadGuesses(Connection conn, int sessionId) throws SQLException {
        // Query to get all guesses for this session
        final String sql =
            "SELECT guess_number, guessed_actor_id, hint_result " +
            "FROM guess " +
            "WHERE session_id = ? " +
            "ORDER BY guess_number ASC";

        List<Guess> guesses = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sessionId);
            // For each guess, load the full Actor details for the guessed actor and build Guess objects
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Actor actor = loadActor(conn, rs.getString("guessed_actor_id"));
                    if (actor != null) {
                        guesses.add(new Guess(
                            rs.getInt("guess_number"),
                            actor,
                            rs.getString("hint_result")
                        ));
                    }
                }
            }
        }
        return guesses;
    }

    /**
     * Build the hint result JSON by comparing the guessed Actor to the answer Actor.
     * @param guessed The Actor object representing the user's guess
     * @param answer The Actor object representing the correct answer for today's game
     * @return String containing the JSON hint result to be stored in the guess row
     */
    public String buildHintResult(Actor guessed, Actor answer) {
        boolean isCorrect = guessed.getActorId().equals(answer.getActorId());

        // Birth year
        String birthHint = "unknown";
        if (guessed.getBirthYear() > 0 && answer.getBirthYear() > 0) {
            if (guessed.getBirthYear() == answer.getBirthYear())      birthHint = "match";
            else if (guessed.getBirthYear() < answer.getBirthYear())  birthHint = "higher";
            else                                                        birthHint = "lower";
        }

        // Death year
        String deathHint = "unknown";
        if (guessed.getDeathYear() <= 0 && answer.getDeathYear() <= 0) {
            deathHint = "both_alive";
        } else if (guessed.getDeathYear() > 0 && answer.getDeathYear() > 0) {
            if (guessed.getDeathYear() == answer.getDeathYear())      deathHint = "match";
            else if (guessed.getDeathYear() < answer.getDeathYear())  deathHint = "higher";
            else                                                        deathHint = "lower";
        }

        // Profession - Split the profession strings into lists and compare for any profession matches (there can be multiple).
        List<String> guessedProfessions = guessed.getProfession() != null
            ? Arrays.asList(guessed.getProfession().toLowerCase().split(",")) : new ArrayList<>();
        List<String> answerProfessions  = answer.getProfession() != null
            ? Arrays.asList(answer.getProfession().toLowerCase().split(",")) : new ArrayList<>();

        StringBuilder incorrectProfessions = new StringBuilder("[");
        StringBuilder correctProfessions = new StringBuilder("[");
        boolean firstCorrectProfession = true;
        boolean firstIncorrectProfession = true;

        for (String p : guessedProfessions) {
            if (answerProfessions.contains(p.trim())) {
                if (!firstCorrectProfession) {
                    correctProfessions.append(",");
                }
                correctProfessions.append("\"").append(p.trim()).append("\"");
                firstCorrectProfession = false;
            } else {
                if (!firstIncorrectProfession) {
                    incorrectProfessions.append(",");
                }
                incorrectProfessions.append("\"").append(p.trim()).append("\"");
                firstIncorrectProfession = false;
            }
        }
        
        correctProfessions.append("]");
        incorrectProfessions.append("]");

        // Shared titles — iterate both Title lists and match on titleId
        StringBuilder titlesJson = new StringBuilder("[");
        boolean firstTitle = true;
        for (Title gt : guessed.getTitles()) {
            for (Title at : answer.getTitles()) {
                if (gt.getTitleId().equals(at.getTitleId())) {
                    if (!firstTitle) titlesJson.append(",");
                    titlesJson.append("\"")
                              .append(gt.getPrimaryName().replace("\"", "\\\""))
                              .append("\"");
                    firstTitle = false;
                }
            }
        }
        titlesJson.append("]");

        return String.format(
            "{\"correct\":%b,\"birth_year\":\"%s\",\"death_year\":\"%s\"," +
            "\"correct_professions\":%s,\"incorrect_professions\":%s,\"shared_titles\":%s}",
            isCorrect, birthHint, deathHint, correctProfessions, incorrectProfessions, titlesJson
        );
    }

    /**
     * Process a user's guess by inserting a new guess row, updating the game_session, and if the game is now over, updating user_stats.
     * @param conn Database connection to use for the queries
     * @param sessionId ID of the current game session
     * @param userId ID of the user making the guess
     * @param guessedActor Actor object representing the user's guessed actor
     * @param answerActor Actor object representing the correct answer for today's game
     * @param nextGuessNumber The guess number for this guess (1-6)
     * @return boolean indicating whether the guess was correct
     * @throws SQLException
     */
    public boolean submitGuess(Connection conn, int sessionId, int userId,
                               Actor guessedActor, Actor answerActor,
                               int nextGuessNumber) throws SQLException {

        String hintResult = buildHintResult(guessedActor, answerActor);
        boolean isCorrect = guessedActor.getActorId().equals(answerActor.getActorId());

        // Insert guess row
        final String insertGuessSql =
            "INSERT INTO guess (session_id, guessed_actor_id, guess_number, hint_result) " +
            "VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(insertGuessSql)) {
            pstmt.setInt(1, sessionId);
            pstmt.setString(2, guessedActor.getActorId());
            pstmt.setInt(3, nextGuessNumber);
            pstmt.setString(4, hintResult);
            pstmt.executeUpdate();
        }

        // Update game_session
        boolean gameOver = isCorrect || nextGuessNumber >= 6;

        final String updateSessionSql =
            "UPDATE game_session SET guesses_used = ?, solved = ? WHERE session_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(updateSessionSql)) {
            pstmt.setInt(1, nextGuessNumber);
            pstmt.setBoolean(2, isCorrect);
            pstmt.setInt(3, sessionId);
            pstmt.executeUpdate();
        }

        // Update user_stats if the game just ended
        if (gameOver) {
            updateStats(conn, userId, isCorrect, nextGuessNumber);
        }

        return isCorrect;
    }

    /**
     * Update the user's statistics in user_stats after a game ends.
     * @param conn  Database connection to use for the queries
     * @param userId ID of the user whose stats to update
     * @param won boolean indicating whether the user won the game
     * @param guessesUsed number of guesses the user used in the game
     * @throws SQLException if a database access error occurs
     */
    private void updateStats(Connection conn, int userId, boolean won, int guessesUsed)
            throws SQLException {

        final String getStatsSql =
            "SELECT games_played, games_won, current_streak, max_streak, avg_guesses " +
            "FROM user_stats WHERE user_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(getStatsSql)) {
            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int gamesPlayed   = rs.getInt("games_played") + 1;
                    int gamesWon      = rs.getInt("games_won") + (won ? 1 : 0);
                    int currentStreak = won ? rs.getInt("current_streak") + 1 : 0;
                    int maxStreak     = Math.max(rs.getInt("max_streak"), currentStreak);
                    double oldAvg     = rs.getDouble("avg_guesses");
                    double newAvg     = ((oldAvg * (gamesPlayed - 1)) + guessesUsed) / gamesPlayed;

                    final String updateSql =
                        "UPDATE user_stats " +
                        "SET games_played = ?, games_won = ?, current_streak = ?, " +
                        "    max_streak = ?, avg_guesses = ? " +
                        "WHERE user_id = ?";

                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setInt(1, gamesPlayed);
                        updateStmt.setInt(2, gamesWon);
                        updateStmt.setInt(3, currentStreak);
                        updateStmt.setInt(4, maxStreak);
                        updateStmt.setDouble(5, newAvg);
                        updateStmt.setInt(6, userId);
                        updateStmt.executeUpdate();
                    }
                }
            }
        }
    }
    // generateDailyGame runs every night at midnight and generates a new game
    @Scheduled(cron = "0 0 0 * * ?")
    public void generateDailyGame() {
        final String sql = "INSERT into daily_game (game_date, actor_id) " +
                           "SELECT " + 
                           "CURDATE(), " + 
                           "a.actor_id " +
                           "FROM actor a " +
                           "ORDER BY RAND() LIMIT 1";

        try (Connection conn = dataSource.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
