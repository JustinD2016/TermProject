package uga.menik.csx370.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import uga.menik.csx370.models.Actor;
import uga.menik.csx370.models.GameSession;
import uga.menik.csx370.services.GameService;
import uga.menik.csx370.services.UserService;

/**
 * Controller for handling the game, we gotta figure out a good name for it guys.
 * Handles the page load and guess submission for the daily actor guessing game. 
 * On page load, it checks if a session exists for the user + today's game, and loads it if found. 
 * If no session exists, it creates a new one. It also handles error messages passed via query params on redirect. 
 * On guess submission, it validates the input, looks up the guessed actor, checks the guess against the answer, updates the session and stats, and redirects back.
 */
@Controller
@RequestMapping("/")
public class GameController {

    private final DataSource dataSource;
    private final UserService userService;
    private final GameService gameService;

    public GameController(DataSource dataSource, UserService userService, GameService gameService) {
        this.dataSource = dataSource;
        this.userService = userService;
        this.gameService = gameService;
    }

    /**
     * Loads the game page. If a session exists for the user + today's game, it is loaded and rendered.
     * Otherwise, a new session is created. If no game is scheduled for today, an appropriate message is shown.
     * @param error Optional error message to display (passed via query param on redirect)
     * @return ModelAndView for the game page, with session data and any error message included in the model
     */
    @GetMapping
    public ModelAndView webpage(@RequestParam(name = "error", required = false) String error) {
        ModelAndView mv = new ModelAndView("game_page");
        int userId = Integer.parseInt(userService.getLoggedInUser().getUserId());

        try (Connection conn = dataSource.getConnection()) {

            // Get today's game_id
            int[] todaysGame = gameService.getTodaysGame(conn);
            int gameId = todaysGame[0];

            if (gameId == -1) {
                mv.addObject("isNoContent", true);
                mv.addObject("errorMessage", "No game scheduled for today. Check back soon!");
                return mv;
            }

            // Find or create the session - loads all prior guesses
            GameSession session = gameService.getOrCreateSession(conn, userId, gameId);

            boolean gameOver = session.isSolved() || session.getGuessesUsed() >= 6;

            mv.addObject("session", session);
            mv.addObject("guessesRemaining", 6 - session.getGuessesUsed());
            mv.addObject("gameOver", gameOver);
            mv.addObject("isNoContent", session.getGuesses().isEmpty());

            // Only reveal the answer actor once the game is over
            if (gameOver) {
                String answerActorId = gameService.getTodaysActorId(conn);
                Actor answerActor = gameService.loadActor(conn, answerActorId);
                mv.addObject("answerActor", answerActor);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            String message = URLEncoder.encode("Failed to load the game. Please try again.", StandardCharsets.UTF_8);
            return new ModelAndView("redirect:/?error=" + message);
        }

        mv.addObject("errorMessage", error);
        return mv;
    }

    /**
     * Handles submission of a guess. Validates input, looks up the guessed actor, checks the guess against the answer,
     * @param actorName The guessed actor name, expected in "First Last" format. The controller will attempt to parse this into first and last name for lookup.
     * @return Redirect back to the game page, with an error message if the guess was invalid or if any issues occurred during processing
     */
    @PostMapping("/guess")
    public String submitGuess(@RequestParam(name = "actorname") String actorName) {

        if (actorName == null || actorName.trim().isEmpty()) {
            String message = URLEncoder.encode("Please enter an actor name.", StandardCharsets.UTF_8);
            return "redirect:/?error=" + message;
        }

        int userId = Integer.parseInt(userService.getLoggedInUser().getUserId());

        try (Connection conn = dataSource.getConnection()) {

            // Get today's game
            int[] todaysGame = gameService.getTodaysGame(conn);
            int gameId = todaysGame[0];

            if (gameId == -1) {
                String message = URLEncoder.encode("No game found for today.", StandardCharsets.UTF_8);
                return "redirect:/?error=" + message;
            }

            // Get current session - check it isn't already over
            GameSession session = gameService.getOrCreateSession(conn, userId, gameId);

            if (session.isSolved() || session.getGuessesUsed() >= 6) {
                String message = URLEncoder.encode("You have already finished today's game.", StandardCharsets.UTF_8);
                return "redirect:/?error=" + message;
            }

            // Look up the guessed actor by first + last name
            String[] nameParts = actorName.trim().split("\\s+", 2);
            String firstName = nameParts[0];
            String lastName  = nameParts.length > 1 ? nameParts[1] : "";

            final String findActorSql =
                "SELECT actor_id FROM actor WHERE first_name = ? AND last_name = ? LIMIT 1";

            String guessedActorId = null;

            try (PreparedStatement pstmt = conn.prepareStatement(findActorSql)) {
                pstmt.setString(1, firstName);
                pstmt.setString(2, lastName);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        guessedActorId = rs.getString("actor_id");
                    }
                }
            }

            if (guessedActorId == null) {
                String message = URLEncoder.encode(
                    "Actor \"" + actorName + "\" not found. Check spelling and try again.",
                    StandardCharsets.UTF_8);
                return "redirect:/?error=" + message;
            }

            // Load both actors (GameService handles the Title lists)
            Actor guessedActor = gameService.loadActor(conn, guessedActorId);
            Actor answerActor  = gameService.loadActor(conn, gameService.getTodaysActorId(conn));

            if (guessedActor == null || answerActor == null) {
                String message = URLEncoder.encode("Failed to load actor data.", StandardCharsets.UTF_8);
                return "redirect:/?error=" + message;
            }

            // Delegate: build hint, insert guess, update session + stats
            int nextGuessNumber = session.getGuessesUsed() + 1;
            gameService.submitGuess(conn, Integer.parseInt(session.getSessionId()),
                userId, guessedActor, answerActor, nextGuessNumber);

        } catch (SQLException e) {
            e.printStackTrace();
            String message = URLEncoder.encode("Failed to submit guess. Please try again.", StandardCharsets.UTF_8);
            return "redirect:/?error=" + message;
        }

        return "redirect:/";
    }

}