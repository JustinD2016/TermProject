package uga.menik.csx370.controllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;


import uga.menik.csx370.services.UserService;

@Controller
@RequestMapping("/profile")
public class ProfileController {
 
    private final DataSource dataSource;
    private final UserService userService;

    @Autowired
    public ProfileController(DataSource dataSource, UserService userService) {
        this.dataSource = dataSource;
        this.userService = userService;
    }
 
    @GetMapping
    public ModelAndView webpage(@RequestParam(name = "error", required = false) String error) {
        ModelAndView mv = new ModelAndView("profile_page");
        int userId = Integer.parseInt(userService.getLoggedInUser().getUserId());
         // System.out.println("logged in userId: " + userId);
        mv.addObject("username", userService.getLoggedInUser().getUsername());
        try (Connection conn = dataSource.getConnection()) {

            final String followingCount = "SELECT COUNT(*) FROM follow WHERE follower_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(followingCount)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    mv.addObject("followingCount", rs.next() ? rs.getInt(1) : 0);
                }
            }
            final String followerCount = "SELECT COUNT(*) FROM follow WHERE followee_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(followerCount)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    mv.addObject("followerCount", rs.next() ? rs.getInt(1):0);
                }
            }
            final String todayResult = "SELECT gs.guesses_used AS guesses_used, gs.solved AS solved FROM game_session gs " +
                                "JOIN daily_game dg ON gs.game_id = dg.game_id " + 
                                "WHERE gs.user_id = ? AND dg.game_date = CURDATE()";
            try(PreparedStatement ps = conn.prepareStatement(todayResult)) {
                ps.setInt(1, userId);
                try(ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        mv.addObject("todayPlayed", true);
                        mv.addObject("todaySolved", rs.getBoolean("solved"));
                        mv.addObject("todayGuessesUsed", rs.getInt("guesses_used"));
                    } else {
                        mv.addObject("todayPlayed", false);
                    }
                }
            }

            final String followingStatusStatement = "SELECT u.username, gs.solved, gs.guesses_used " + 
            "FROM follow f JOIN user u ON f.followee_id = user_id " + 
            "LEFT JOIN game_session gs ON gs.user_id = u.user_id " + 
            "AND gs.game_id = (SELECT game_id FROM daily_game WHERE game_date = CURDATE()) " +
            "WHERE f.follower_id = ?";
            
            List<Map<String, Object>> followingStatus = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(followingStatusStatement)) {
            ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> entry = new HashMap<>();
                        entry.put("username", rs.getString("username"));
                        boolean played = rs.getObject("solved") != null;
                        entry.put("played", played);
                        entry.put("solved", played && rs.getBoolean("solved"));
                        entry.put("guessesUsed", played ? rs.getInt("guesses_used") : 0);
                        followingStatus.add(entry);
                    }
                }
            }
            mv.addObject("followingStatus", followingStatus);
       
            final String streak = "SELECT current_streak, max_streak FROM user_stats WHERE user_id = ?";

            try (PreparedStatement ps = conn.prepareStatement(streak)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                    mv.addObject("currentStreak", rs.getInt("current_streak"));
                    mv.addObject("maxStreak", rs.getInt("max_streak"));
                } else {
            mv.addObject("currentStreak", 0);
            mv.addObject("maxStreak", 0);
        }
    }
}
        } catch (SQLException e) {
        e.printStackTrace();
        mv.addObject("errorMessage", "Failed to load profile data.");
   
        }
        mv.addObject("errorMessage", error);
        return mv;
 
    }
}
