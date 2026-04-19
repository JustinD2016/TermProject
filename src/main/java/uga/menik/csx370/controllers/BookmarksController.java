/**
Copyright (c) 2024 Sami Menik, PhD. All rights reserved.

This is a project developed by Dr. Menik to give the students an opportunity to apply database concepts learned in the class in a real world project. Permission is granted to host a running version of this software and to use images or videos of this work solely for the purpose of demonstrating the work to potential employers. Any form of reproduction, distribution, or transmission of the software's source code, in part or whole, without the prior written consent of the copyright owner, is strictly prohibited.
*/
package uga.menik.csx370.controllers;

import java.util.List;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import uga.menik.csx370.models.Post;
import uga.menik.csx370.utility.Utility;
import uga.menik.csx370.services.UserService;

/**
 * Handles /bookmarks and its sub URLs.
 * No other URLs at this point.
 * 
 * Learn more about @Controller here: 
 * https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller.html
 */
@Controller
@RequestMapping("/bookmarks")
public class BookmarksController {

    private final DataSource datasource;
    private final UserService userService;
    /**
     * /bookmarks URL itself is handled by this.
     */
    public BookmarksController(DataSource datasource, UserService userService) {
        this.datasource = datasource;
        this.userService = userService;
    }
    @GetMapping
    public ModelAndView webpage() {
        // posts_page is a mustache template from src/main/resources/templates.
        // ModelAndView class enables initializing one and populating placeholders
        // in the template using Java objects assigned to named properties.
        ModelAndView mv = new ModelAndView("posts_page");

        // Following line populates sample data.
        // You should replace it with actual data from the database.
        final String sql = "SELECT " +
            "p.postId, " +
            "p.userId, " +
            "u.firstName, " +
            "u.lastName, " +
            "p.content, " +
            "DATE_FORMAT(p.postDate, '%b %d, %Y, %h:%i %p') AS postDate,  " +
            "(SELECT COUNT(*) FROM heart h WHERE h.postId = p.postId) AS heartsCount, " +
            "(SELECT COUNT(*) FROM comment c WHERE c.postId = p.postId) AS commentsCount, " +
            "(SELECT COUNT(*) FROM bookmark b WHERE b.postId = p.postId AND b.userId = ?) AS isBookmarked, " +
            "(SELECT COUNT(*) FROM heart h WHERE h.postId = p.postId AND h.userId = ?) AS isHearted " +
            "FROM post p " +
            "JOIN user u ON p.userId = u.userId " +
            "WHERE p.postId IN (SELECT postId FROM bookmark WHERE userId = ?) " +
            "ORDER BY p.postDate DESC";;
        try (Connection conn = datasource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userService.getLoggedInUser().getUserId()); // Replace with actual logged in user ID
            stmt.setString(2, userService.getLoggedInUser().getUserId()); // Replace with actual logged in user ID
            stmt.setString(3, userService.getLoggedInUser().getUserId()); // Replace with actual logged in user ID
            try (ResultSet rs = stmt.executeQuery()) {
                List<Post> posts = Utility.convertResultSetToPostList(rs);
                mv.addObject("posts", posts);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle the exception as needed, maybe set an error message in the ModelAndView
        }

        // If an error occured, you can set the following property with the
        // error message to show the error message to the user.
        // String errorMessage = "Some error occured!";
        // mv.addObject("errorMessage", errorMessage);

        // Enable the following line if you want to show no content message.
        // Do that if your content list is empty.
        // mv.addObject("isNoContent", true);

        return mv;
    }
    
}
