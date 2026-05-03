/**
Copyright (c) 2024 Sami Menik, PhD. All rights reserved.

This is a project developed by Dr. Menik to give the students an opportunity to apply database concepts learned in the class in a real world project. Permission is granted to host a running version of this software and to use images or videos of this work solely for the purpose of demonstrating the work to potential employers. Any form of reproduction, distribution, or transmission of the software's source code, in part or whole, without the prior written consent of the copyright owner, is strictly prohibited.
*/
package uga.menik.csx370.controllers;

import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.SQLException;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;


import javax.sql.DataSource;

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
 * This controller handles the home page and some of it's sub URLs.
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

    @GetMapping
    public ModelAndView webpage(@RequestParam(name = "error", required = false) String error) {
        ModelAndView mv = new ModelAndView("feed_page");

        // Following line populates sample data.
        // You should replace it with actual data from the database.
        final String sql = "SELECT " +
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
            PreparedStatement pstmt = conn.prepareStatement(sql)) {

        String loggedInUserId = userService.getLoggedInUser().getUserId();
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
