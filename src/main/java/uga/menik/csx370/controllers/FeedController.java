package uga.menik.csx370.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Connection;
import javax.sql.DataSource;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import uga.menik.csx370.models.Feed;
import uga.menik.csx370.models.Guess;
import uga.menik.csx370.services.GameService;
import uga.menik.csx370.services.UserService;

import java.util.ArrayList;

/**
 * Handles /feed URL and its sub urls.
 */
@Controller
@RequestMapping("/feed")
public class FeedController {

    private final DataSource dataSource;
    private final UserService userService;
    private final GameService gameService;

    public FeedController(DataSource dataSource, UserService userService, GameService gameService) {
        this.dataSource = dataSource;
        this.userService = userService;
        this.gameService = gameService;
    }

    /**
     * This function handles the /polls URL.
     */
    @GetMapping
    public ModelAndView webpage(@RequestParam(name = "error", required = false) String error) {
        ModelAndView mv = new ModelAndView("feed_page");

        /**
         * String sql stores the sql command to reutrn the game session id, the number of guesses used, whether 
         * or not the game was solved, the time the game was played, the username of the user who played the game,
         * their user id, the first and last name of the actor guessed, for all games played by the logged in user
         * and users they follow. The list of games is ordered by the time that the game was played, with the most
         * recent games first.
         */
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

        try (Connection conn = dataSource.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(feedSql)) {

        String loggedInUserId = userService.getLoggedInUser().getUserId();
        // Games of players the logged in user follows and their own games.
        pstmt.setInt(1, Integer.parseInt(loggedInUserId));
        pstmt.setInt(2, Integer.parseInt(loggedInUserId));


        try (ResultSet rs = pstmt.executeQuery()) {
            List<Feed> feedGames = new ArrayList<>();
            while (rs.next()) {
                int sessionId = rs.getInt("session_id");
                List<Guess> guesses = gameService.loadGuesses(conn, sessionId);
                feedGames.add(new Feed(
                    rs.getString("username"),
                    rs.getString("user_id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getBoolean("solved"),
                    rs.getInt("guesses_used"),
                    rs.getString("played_at"),
                    guesses
                ));
            }
            if (feedGames.isEmpty()) {
                mv.addObject("isNoContent", true);
            }
            mv.addObject("feedGames", feedGames);
        }

        } catch (SQLException e) {
            e.printStackTrace();
            String message = URLEncoder.encode("Failed to load feed. Please try again.", StandardCharsets.UTF_8);
            return new ModelAndView("redirect:/feed?error=" + message);
        }

        String errorMessage = error;
        mv.addObject("errorMessage", errorMessage);

        return mv;
    }
}
