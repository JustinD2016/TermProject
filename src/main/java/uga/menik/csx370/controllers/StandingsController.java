package uga.menik.csx370.controllers;

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

    @GetMapping
    public ModelAndView webpage(
            @RequestParam(name = "filter", required = false, defaultValue = "all") String filter,
            @RequestParam(name = "error", required = false) String error) {

        ModelAndView mv = new ModelAndView("standings_page");
        int loggedInUserId = Integer.parseInt(userService.getLoggedInUser().getUserId());
        String currentUsername = userService.getLoggedInUser().getUsername();

        final String allUsersSql =
            "SELECT u.username, " +
            "us.games_played, us.games_won, " +
            "ROUND(us.games_won * 100.0 / NULLIF(us.games_played, 0), 1) AS win_rate, " +
            "us.current_streak, us.max_streak, us.avg_guesses, " +
            "RANK() OVER (ORDER BY us.games_won DESC, us.avg_guesses ASC) AS user_rank " +
            "FROM user_stats us JOIN user u ON us.user_id = u.user_id " +
            "ORDER BY user_rank ASC LIMIT 100";

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

        // Query current user's own stats for the pie chart
        final String myStatsSql =
            "SELECT games_won, (games_played - games_won) AS games_lost " +
            "FROM user_stats WHERE user_id = ?";

        boolean isFriendsFilter = "friends".equalsIgnoreCase(filter);
        List<UserStats> standings = new ArrayList<>();
        int myWins = 0;
        int myLosses = 0;

        try (Connection conn = dataSource.getConnection()) {

            // Load leaderboard
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
                        rs.getInt("user_rank")
                    ));
                }
            }
            pstmt.close();

            // Load current user's stats for pie chart
            try (PreparedStatement ps2 = conn.prepareStatement(myStatsSql)) {
                ps2.setInt(1, loggedInUserId);
                try (ResultSet rs2 = ps2.executeQuery()) {
                    if (rs2.next()) {
                        myWins = rs2.getInt("games_won");
                        myLosses = rs2.getInt("games_lost");
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            mv.addObject("standings", standings);
            mv.addObject("isNoContent", true);
            mv.addObject("isFriendsFilter", isFriendsFilter);
            mv.addObject("errorMessage", "Failed to load standings: " + e.getMessage());
            return mv;
        }

        mv.addObject("standings", standings);
        mv.addObject("isNoContent", standings.isEmpty());
        mv.addObject("isFriendsFilter", isFriendsFilter);
        mv.addObject("errorMessage", error);
        mv.addObject("currentUsername", currentUsername);
        mv.addObject("myWins", myWins);
        mv.addObject("myLosses", myLosses);

        return mv;
    }
}