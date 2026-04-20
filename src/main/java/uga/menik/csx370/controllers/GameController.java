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
 * Handles HTTP for the game page — the home page of ActorDle.
 * All business logic is delegated to GameService.
 *
 * GET  /       -> load today's session and render the game page
 * POST /guess  -> submit a guess and redirect back
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

    // -------------------------------------------------------------------------
    // GET /
    // -------------------------------------------------------------------------
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

            // Find or create the session — loads all prior guesses
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

    // -------------------------------------------------------------------------
    // POST /guess
    // -------------------------------------------------------------------------
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

            // Get current session — check it isn't already over
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