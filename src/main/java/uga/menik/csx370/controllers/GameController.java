package uga.menik.csx370.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import uga.menik.csx370.services.UserService;

/**
 * This controller handles the game page and its sub URLs.
 * 
 * GET  /game        -> loads today's game session for the logged-in user
 * POST /game/guess  -> submits a guess and returns hint feedback
 */
@Controller
@RequestMapping("/game")
public class GameController {

    private final DataSource dataSource;
    private final UserService userService;

    public GameController(DataSource dataSource, UserService userService) {
        this.dataSource = dataSource;
        this.userService = userService;
    }

    /**
     * Handles GET /game
     * 
     * Loads today's daily_game, finds or creates a game_session for the
     * logged-in user, then fetches all guesses made so far in that session.
     * Passes everything to the game_page template.
     */
    @GetMapping
    public ModelAndView webpage(@RequestParam(name = "error", required = false) String error) {
        ModelAndView mv = new ModelAndView("game_page");

        int userId = Integer.parseInt(userService.getLoggedInUser().getUserId());

        // -- Step 1: Get today's daily game --
        final String getDailyGameSql =
            "SELECT game_id, actor_id FROM daily_game WHERE game_date = CURDATE()";

        int gameId = -1;
        String answerActorId = null;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(getDailyGameSql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                gameId = rs.getInt("game_id");
                answerActorId = rs.getString("actor_id");
            } else {
                // No game scheduled for today
                mv.addObject("isNoContent", true);
                mv.addObject("errorMessage", "No game available for today. Check back soon!");
                return mv;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            String message = URLEncoder.encode("Failed to load today's game. Please try again.", StandardCharsets.UTF_8);
            return new ModelAndView("redirect:/game?error=" + message);
        }

        // -- Step 2: Find or create a game_session for this user + today's game --
        final String getSessionSql =
            "SELECT session_id, guesses_used, solved FROM game_session " +
            "WHERE user_id = ? AND game_id = ?";

        int sessionId = -1;
        int guessesUsed = 0;
        boolean solved = false;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(getSessionSql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, gameId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    sessionId = rs.getInt("session_id");
                    guessesUsed = rs.getInt("guesses_used");
                    solved = rs.getBoolean("solved");
                }
            }

            // No session yet — create one
            if (sessionId == -1) {
                final String insertSessionSql =
                    "INSERT INTO game_session (user_id, game_id, guesses_used, solved) " +
                    "VALUES (?, ?, 0, 0)";

                try (PreparedStatement insertStmt = conn.prepareStatement(
                        insertSessionSql, PreparedStatement.RETURN_GENERATED_KEYS)) {

                    insertStmt.setInt(1, userId);
                    insertStmt.setInt(2, gameId);
                    insertStmt.executeUpdate();

                    try (ResultSet keys = insertStmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            sessionId = keys.getInt(1);
                        }
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            String message = URLEncoder.encode("Failed to load your game session. Please try again.", StandardCharsets.UTF_8);
            return new ModelAndView("redirect:/game?error=" + message);
        }

        // -- Step 3: Fetch all guesses made so far in this session --
        final String getGuessesSql =
            "SELECT g.guess_number, " +
            "       a.first_name, " +
            "       a.last_name, " +
            "       a.birth_year, " +
            "       a.death_year, " +
            "       a.primary_profession, " +
            "       g.hint_result " +
            "FROM guess g " +
            "JOIN actor a ON a.actor_id = g.guessed_actor_id " +
            "WHERE g.session_id = ? " +
            "ORDER BY g.guess_number ASC";

        List<String[]> guesses = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(getGuessesSql)) {

            pstmt.setInt(1, sessionId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // Each entry: [guessNumber, firstName, lastName, birthYear,
                    //              deathYear, profession, hintResultJson]
                    guesses.add(new String[]{
                        rs.getString("guess_number"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("birth_year"),
                        rs.getString("death_year"),
                        rs.getString("primary_profession"),
                        rs.getString("hint_result")
                    });
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            String message = URLEncoder.encode("Failed to load guesses. Please try again.", StandardCharsets.UTF_8);
            return new ModelAndView("redirect:/game?error=" + message);
        }

        // -- Step 4: Pass data to template --
        mv.addObject("sessionId", sessionId);
        mv.addObject("guessesUsed", guessesUsed);
        mv.addObject("guessesRemaining", 6 - guessesUsed);
        mv.addObject("solved", solved);
        mv.addObject("guesses", guesses);
        mv.addObject("gameOver", solved || guessesUsed >= 6);
        mv.addObject("errorMessage", error);

        // Only reveal the answer if the game is over
        if (solved || guessesUsed >= 6) {
            mv.addObject("answerActorId", answerActorId);
        }

        return mv;
    }

    /**
     * Handles POST /game/guess
     * 
     * Receives the guessed actor name from the form, looks up that actor,
     * compares attributes against today's answer, builds a hint_result JSON
     * string, inserts the guess row, and updates the session.
     * 
     * Form parameter: "actorname" — the full name typed by the user.
     */
    @PostMapping("/guess")
    public String submitGuess(@RequestParam(name = "actorname") String actorName) {

        if (actorName == null || actorName.trim().isEmpty()) {
            String message = URLEncoder.encode("Please enter an actor name.", StandardCharsets.UTF_8);
            return "redirect:/game?error=" + message;
        }

        int userId = Integer.parseInt(userService.getLoggedInUser().getUserId());

        try (Connection conn = dataSource.getConnection()) {

            // -- Step 1: Get today's game and answer actor --
            int gameId = -1;
            String answerActorId = null;
            int answerBirthYear = -1;
            int answerDeathYear = -1;
            String answerProfession = null;

            final String getDailyGameSql =
                "SELECT dg.game_id, dg.actor_id, " +
                "       a.birth_year, a.death_year, a.primary_profession " +
                "FROM daily_game dg " +
                "JOIN actor a ON a.actor_id = dg.actor_id " +
                "WHERE dg.game_date = CURDATE()";

            try (PreparedStatement pstmt = conn.prepareStatement(getDailyGameSql);
                 ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    gameId = rs.getInt("game_id");
                    answerActorId = rs.getString("actor_id");
                    answerBirthYear = rs.getInt("birth_year");
                    answerDeathYear = rs.getInt("death_year");
                    answerProfession = rs.getString("primary_profession");
                } else {
                    String message = URLEncoder.encode("No game found for today.", StandardCharsets.UTF_8);
                    return "redirect:/game?error=" + message;
                }
            }

            // -- Step 2: Get the current session --
            int sessionId = -1;
            int guessesUsed = 0;
            boolean alreadySolved = false;

            final String getSessionSql =
                "SELECT session_id, guesses_used, solved FROM game_session " +
                "WHERE user_id = ? AND game_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(getSessionSql)) {
                pstmt.setInt(1, userId);
                pstmt.setInt(2, gameId);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        sessionId = rs.getInt("session_id");
                        guessesUsed = rs.getInt("guesses_used");
                        alreadySolved = rs.getBoolean("solved");
                    }
                }
            }

            // Guard: game already over
            if (alreadySolved || guessesUsed >= 6) {
                String message = URLEncoder.encode("You have already finished today's game.", StandardCharsets.UTF_8);
                return "redirect:/game?error=" + message;
            }

            // -- Step 3: Look up the guessed actor by name --
            String guessedActorId = null;
            int guessedBirthYear = -1;
            int guessedDeathYear = -1;
            String guessedProfession = null;

            // Split name into first / last (simple split on first space)
            String[] nameParts = actorName.trim().split("\\s+", 2);
            String firstName = nameParts[0];
            String lastName = nameParts.length > 1 ? nameParts[1] : "";

            final String findActorSql =
                "SELECT actor_id, birth_year, death_year, primary_profession " +
                "FROM actor " +
                "WHERE first_name = ? AND last_name = ? " +
                "LIMIT 1";

            try (PreparedStatement pstmt = conn.prepareStatement(findActorSql)) {
                pstmt.setString(1, firstName);
                pstmt.setString(2, lastName);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        guessedActorId = rs.getString("actor_id");
                        guessedBirthYear = rs.getInt("birth_year");
                        guessedDeathYear = rs.getInt("death_year");
                        guessedProfession = rs.getString("primary_profession");
                    }
                }
            }

            if (guessedActorId == null) {
                String message = URLEncoder.encode(
                    "Actor \"" + actorName + "\" not found. Check the spelling and try again.",
                    StandardCharsets.UTF_8);
                return "redirect:/game?error=" + message;
            }

            // -- Step 4: Compare attributes and build hint_result JSON --
            // birth_year: "match" | "higher" (answer born later) | "lower" | "unknown"
            String birthHint = "unknown";
            if (answerBirthYear > 0 && guessedBirthYear > 0) {
                if (guessedBirthYear == answerBirthYear)      birthHint = "match";
                else if (guessedBirthYear < answerBirthYear)  birthHint = "higher";
                else                                           birthHint = "lower";
            }

            // death_year: "match" | "higher" | "lower" | "alive" | "unknown"
            String deathHint = "unknown";
            if (answerDeathYear <= 0 && guessedDeathYear <= 0) {
                deathHint = "alive";                          // both alive
            } else if (answerDeathYear > 0 && guessedDeathYear > 0) {
                if (guessedDeathYear == answerDeathYear)      deathHint = "match";
                else if (guessedDeathYear < answerDeathYear)  deathHint = "higher";
                else                                          deathHint = "lower";
            }

            // profession: "match" | "no_match"
            String professionHint = "no_match";
            if (answerProfession != null && answerProfession.equalsIgnoreCase(guessedProfession)) {
                professionHint = "match";
            }

            // Shared titles between guessed actor and answer actor
            final String sharedTitlesSql =
                "SELECT t.primary_title " +
                "FROM actor_title at1 " +
                "JOIN actor_title at2 ON at1.title_id = at2.title_id " +
                "JOIN title t ON t.title_id = at1.title_id " +
                "WHERE at1.actor_id = ? AND at2.actor_id = ?";

            StringBuilder sharedTitlesJson = new StringBuilder("[");
            try (PreparedStatement pstmt = conn.prepareStatement(sharedTitlesSql)) {
                pstmt.setString(1, guessedActorId);
                pstmt.setString(2, answerActorId);

                try (ResultSet rs = pstmt.executeQuery()) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) sharedTitlesJson.append(",");
                        // Escape quotes in title names for valid JSON
                        String title = rs.getString("primary_title").replace("\"", "\\\"");
                        sharedTitlesJson.append("\"").append(title).append("\"");
                        first = false;
                    }
                }
            }
            sharedTitlesJson.append("]");

            // Assemble the hint_result JSON string
            boolean isCorrect = guessedActorId.equals(answerActorId);
            String hintResult = String.format(
                "{\"correct\":%b,\"birth_year\":\"%s\",\"death_year\":\"%s\"," +
                "\"profession\":\"%s\",\"shared_titles\":%s}",
                isCorrect, birthHint, deathHint, professionHint, sharedTitlesJson
            );

            // -- Step 5: Insert the guess row --
            int nextGuessNumber = guessesUsed + 1;

            final String insertGuessSql =
                "INSERT INTO guess (session_id, guessed_actor_id, guess_number, hint_result) " +
                "VALUES (?, ?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(insertGuessSql)) {
                pstmt.setInt(1, sessionId);
                pstmt.setString(2, guessedActorId);
                pstmt.setInt(3, nextGuessNumber);
                pstmt.setString(4, hintResult);
                pstmt.executeUpdate();
            }

            // -- Step 6: Update the game_session --
            boolean nowSolved = isCorrect;
            boolean gameOver = nowSolved || nextGuessNumber >= 6;

            final String updateSessionSql =
                "UPDATE game_session SET guesses_used = ?, solved = ? " +
                "WHERE session_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(updateSessionSql)) {
                pstmt.setInt(1, nextGuessNumber);
                pstmt.setBoolean(2, nowSolved);
                pstmt.setInt(3, sessionId);
                pstmt.executeUpdate();
            }

            // -- Step 7: Update user_stats if the game just ended --
            if (gameOver) {
                final String getStatsSql =
                    "SELECT games_played, games_won, current_streak, max_streak, avg_guesses " +
                    "FROM user_stats WHERE user_id = ?";

                try (PreparedStatement pstmt = conn.prepareStatement(getStatsSql)) {
                    pstmt.setInt(1, userId);

                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            int gamesPlayed   = rs.getInt("games_played") + 1;
                            int gamesWon      = rs.getInt("games_won") + (nowSolved ? 1 : 0);
                            int currentStreak = nowSolved ? rs.getInt("current_streak") + 1 : 0;
                            int maxStreak     = Math.max(rs.getInt("max_streak"), currentStreak);
                            // Rolling average: newAvg = (oldAvg * (n-1) + newGuesses) / n
                            double oldAvg     = rs.getDouble("avg_guesses");
                            double newAvg     = ((oldAvg * (gamesPlayed - 1)) + nextGuessNumber) / gamesPlayed;

                            final String updateStatsSql =
                                "UPDATE user_stats " +
                                "SET games_played = ?, games_won = ?, current_streak = ?, " +
                                "    max_streak = ?, avg_guesses = ? " +
                                "WHERE user_id = ?";

                            try (PreparedStatement updateStmt = conn.prepareStatement(updateStatsSql)) {
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

        } catch (SQLException e) {
            e.printStackTrace();
            String message = URLEncoder.encode("Failed to submit guess. Please try again.", StandardCharsets.UTF_8);
            return "redirect:/game?error=" + message;
        }

        return "redirect:/game";
    }

}
