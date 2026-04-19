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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import uga.menik.csx370.models.Post;
import uga.menik.csx370.services.UserService;
import uga.menik.csx370.utility.Utility;

/**
 * Handles /profile URL and its sub URLs.
 */
@Controller
@RequestMapping("/profile")
public class ProfileController {

    // UserService has user login and registration related functions.
    private final UserService userService;
    private final DataSource dataSource;

    /**
     * See notes in AuthInterceptor.java regarding how this works 
     * through dependency injection and inversion of control.
     */
    @Autowired
    public ProfileController(UserService userService, DataSource dataSource) {
        this.userService = userService;
        this.dataSource = dataSource;
    }

    /**
     * This function handles /profile URL itself.
     * This serves the webpage that shows posts of the logged in user.
     */
    @GetMapping
    public ModelAndView profileOfLoggedInUser() {
        System.out.println("User is attempting to view profile of the logged in user.");
        return profileOfSpecificUser(userService.getLoggedInUser().getUserId());
    }

    /**
     * This function handles /profile/{userId} URL.
     * This serves the webpage that shows posts of a speific user given by userId.
     * See comments in PeopleController.java in followUnfollowUser function regarding 
     * how path variables work.
     */
    @GetMapping("/{userId}")
    public ModelAndView profileOfSpecificUser(@PathVariable("userId") String userId) {
        System.out.println("User is attempting to view profile: " + userId);

        // See notes on ModelAndView in BookmarksController.java.
        ModelAndView mv = new ModelAndView("posts_page");

        final String sql = "SELECT " +
            "p.postId, " +
            "p.userId, " +
            "u.firstName, " +
            "u.lastName, " +
            "p.content, " +
            "DATE_FORMAT(p.postDate, '%b %d, %Y, %h:%i %p') AS postDate,  " +
            "(SELECT COUNT(*) FROM heart h WHERE h.postId = p.postId) AS heartsCount, " +
            "(SELECT COUNT(*) FROM comment c WHERE c.postId = p.postId) AS commentsCount, " +
            "(CASE WHEN EXISTS (SELECT 1 FROM heart h2 WHERE h2.postId = p.postId AND h2.userId = ?) THEN true ELSE false END) AS isHearted, " +
            "(CASE WHEN EXISTS (SELECT 1 FROM bookmark b WHERE b.postId = p.postId AND b.userId = ?) THEN true ELSE false END) AS isBookmarked " +
            "FROM post p JOIN user u ON p.userId = u.userId WHERE p.userId = ? ORDER BY p.postDate DESC";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userService.getLoggedInUser().getUserId());
            pstmt.setString(2, userService.getLoggedInUser().getUserId());
            pstmt.setString(3, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                List<Post> postsForUser = Utility.convertResultSetToPostList(rs);
                mv.addObject("posts", postsForUser);
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
