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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import uga.menik.csx370.models.UserStats;
import uga.menik.csx370.services.UserService;

/**
 * Controller for the standings/leaderboard page at /standings.
 * Shows global rankings and optionally filters to only followed users.
 */
@Controller
@RequestMapping("/standings")
public class StandingsController {

    private final DataSource dataSource;
    private final UserService userService;

    @Autowired
    public StandingsController(DataSource dataSource, UserService userService) {
        this.dataSource = dataSource;
        this.userService = userService;
    }

    /**
     * Serves the standings page. Supports filtering between all users and
     * only users the logged-in user follows via the "filter" query param.
     *
     * @param filter "friends" to show only followed users, anything else shows all
     * @param error  Optional error message passed via query param
     */
    @GetMapping
    public ModelAndView webpage(
            @RequestParam(name = "filter", required = false, defaultValue = "all") String filter,
            @RequestParam(name = "error", required = false) String error) {

        ModelAndView mv = new ModelAndView("standings_page");
        int loggedInUserId = Integer.parseInt(userService.getLoggedInUser().getUserId());

        // Two queries: one for all users, one filtered to friends only
        final String allUsersSql =
            "SELECT u.username, " +
            "       us.games_played, " +
            "       us.games_won, " +
            "       ROUND(us.games_won * 100.0 / NULLIF(us.games_played, 0), 1) AS win_rate, " +
            "       us.current_streak, " +
            "       us.max_streak, " +
            "       us.avg_guesses, " +
            "       RANK() OVER (ORDER BY us.games_won DESC, us.avg_guesses ASC) AS rank " +
            "FROM user_stats us " +
            "JOIN user u ON us.user_id = u.user_id " +
            "ORDER BY rank ASC " +
            "LIMIT 100";

        final String friendsSql =
            "SELECT u.username, " +
            "       us.games_played, " +
            "       us.games_won, " +
            "       ROUND(us.games_won * 100.0 / NULLIF(us.games_played, 0), 1) AS win_rate, " +
            "       us.current_streak, " +
            "       us.max_streak, " +
            "       us.avg_guesses, " +
            "       RANK() OVER (ORDER BY us.games_won DESC, us.avg_guesses ASC) AS rank " +
            "FROM user_stats us " +
            "JOIN user u ON us.user_id = u.user_id " +
            "WHERE us.user_id = ? " +
            "   OR us.user_id IN ( " +
            "       SELECT followee_id FROM follow WHERE follower_id = ? " +
            "   ) " +
            "ORDER BY rank ASC";

        boolean isFriendsFilter = "friends".equalsIgnoreCase(filter);
        List<UserStats> standings = new ArrayList<>();

        try (Connection conn = dataSource.getConnection()) {

            PreparedStatement pstmt;
            if (isFriendsFilter) {
                pstmt = conn.prepareStatement(friendsSql);
                pstmt.setInt(1, loggedInUserId);
                pstmt.setInt(2, loggedInUserId);
            } else {
                pstmt = conn.prepareStatement(allUsersSql);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    standings.add(new UserStats(
                        rs.getString("username"),
                        rs.getInt("games_played"),
                        rs.getInt("games_won"),
                        rs.getDouble("win_rate"),
                        rs.getInt("current_streak"),
                        rs.getInt("max_streak"),
                        rs.getDouble("avg_guesses"),
                        rs.getInt("rank")
                    ));
                }
            }

            pstmt.close();

        } catch (SQLException e) {
            e.printStackTrace();
            String message = URLEncoder.encode("Failed to load standings. Please try again.",
                    StandardCharsets.UTF_8);
            return new ModelAndView("redirect:/standings?error=" + message);
        }

        mv.addObject("standings", standings);
        mv.addObject("isNoContent", standings.isEmpty());
        mv.addObject("isFriendsFilter", isFriendsFilter);
        mv.addObject("errorMessage", error);

        return mv;
    }
}