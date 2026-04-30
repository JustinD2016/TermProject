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
import uga.menik.csx370.services.GameService;
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
       
        } catch (SQLException e) {
        e.printStackTrace();
        mv.addObject("errorMessage", "Failed to load profile data.");
   
        }
        mv.addObject("errorMessage", error);
        return mv;
 
    }
}
