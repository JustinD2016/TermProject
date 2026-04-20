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

    // -------------------------------------------------------------------------
    // Load an Actor with their full Title list by actor_id.
    // Returns null if the actor is not found.
    // Used both when loading prior guesses and when looking up the answer actor.
    // -------------------------------------------------------------------------
    public Actor loadActor(Connection conn, String actorId) throws SQLException {
        final String actorSql =
            "SELECT actor_id, first_name, last_name, primary_profession, " +
            "       birth_year, death_year " +
            "FROM actor WHERE actor_id = ?";

        final String titleSql =
            "SELECT t.title_id, t.primary_title, t.title_type, t.start_year, t.genres " +
            "FROM actor_title at " +
            "JOIN title t ON t.title_id = at.title_id " +
            "WHERE at.actor_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(actorSql)) {
            pstmt.setString(1, actorId);

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

    // -------------------------------------------------------------------------
    // Find today's daily_game row.
    // Returns int[]{gameId} with gameId = -1 if no game is scheduled today.
    // -------------------------------------------------------------------------
    public int[] getTodaysGame(Connection conn) throws SQLException {
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

    // -------------------------------------------------------------------------
    // Find today's answer actor_id.
    // -------------------------------------------------------------------------
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

    // -------------------------------------------------------------------------
    // Find or create a GameSession for this user + today's game.
    // Loads all existing guesses for the session.
    // -------------------------------------------------------------------------
    public GameSession getOrCreateSession(Connection conn, int userId, int gameId)
            throws SQLException {

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

        // No session yet — create one
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

    // -------------------------------------------------------------------------
    // Load all Guess objects for a session, each with a full Actor.
    // -------------------------------------------------------------------------
    private List<Guess> loadGuesses(Connection conn, int sessionId) throws SQLException {
        final String sql =
            "SELECT guess_number, guessed_actor_id, hint_result " +
            "FROM guess " +
            "WHERE session_id = ? " +
            "ORDER BY guess_number ASC";

        List<Guess> guesses = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sessionId);

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

    // -------------------------------------------------------------------------
    // Compare a guessed Actor against the answer Actor and build a
    // hint result JSON string.
    //
    // birth_year:  "match" | "higher" (answer born later) | "lower" | "unknown"
    // death_year:  "match" | "higher" | "lower" | "both_alive" | "unknown"
    // profession:  "match" | "no_match"
    // correct:     true if this guess is the answer
    // shared_titles: JSON array of title names that appear in both actors' lists
    // -------------------------------------------------------------------------
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

        // Profession
        String profHint = "no_match";
        if (guessed.getProfession() != null &&
            guessed.getProfession().equalsIgnoreCase(answer.getProfession())) {
            profHint = "match";
        }

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
            "\"profession\":\"%s\",\"shared_titles\":%s}",
            isCorrect, birthHint, deathHint, profHint, titlesJson
        );
    }

    // -------------------------------------------------------------------------
    // Submit a guess: insert the guess row, update the session,
    // and update user_stats if the game just ended.
    //
    // Returns true if the guess was the correct answer.
    // -------------------------------------------------------------------------
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

    // -------------------------------------------------------------------------
    // Update user_stats after a game ends.
    // Recalculates win rate, streak, and rolling average guesses.
    // -------------------------------------------------------------------------
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

}
