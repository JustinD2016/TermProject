/**
Copyright (c) 2024 Sami Menik, PhD. All rights reserved.

This is a project developed by Dr. Menik to give the students an opportunity to apply database concepts learned in the class in a real world project. Permission is granted to host a running version of this software and to use images or videos of this work solely for the purpose of demonstrating the work to potential employers. Any form of reproduction, distribution, or transmission of the software's source code, in part or whole, without the prior written consent of the copyright owner, is strictly prohibited.
*/
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import uga.menik.csx370.models.ExpandedPost;
import uga.menik.csx370.utility.Utility;
import uga.menik.csx370.services.UserService;

/**
 * Handles /post URL and its sub urls.
 */
@Controller
@RequestMapping("/post")
public class PostController {

    private final UserService userService;
    private final DataSource dataSource;

    public PostController(UserService userService, DataSource dataSource) {
        this.userService = userService;
        this.dataSource = dataSource;
    }

    /**
     * This function handles the /post/{postId} URL.
     * This handlers serves the web page for a specific post.
     * Note there is a path variable {postId}.
     * An example URL handled by this function looks like below:
     * http://localhost:8081/post/1
     * The above URL assigns 1 to postId.
     * 
     * See notes from HomeController.java regardig error URL parameter.
     */
    @GetMapping("/{postId}")
    public ModelAndView webpage(@PathVariable("postId") String postId,
            @RequestParam(name = "error", required = false) String error) {
        System.out.println("The user is attempting to view post with id: " + postId);
        // See notes on ModelAndView in BookmarksController.java.
        ModelAndView mv = new ModelAndView("posts_page");

        final String sql = "SELECT " +
            "p.postId, " +
            "p.userId, " +
            "u.firstName, " +
            "u.lastName, " +
            "p.content, " +
            "DATE_FORMAT(p.postDate, '%b %d, %Y, %h:%i %p') AS postDate, " +
            "(SELECT COUNT(*) FROM heart h WHERE h.postId = p.postId) AS heartsCount, " +
            "(SELECT COUNT(*) FROM comment c WHERE c.postId = p.postId) AS commentsCount, " +
            "(CASE WHEN EXISTS (SELECT 1 FROM heart h2 WHERE h2.postId = p.postId AND h2.userId = ?) THEN true ELSE false END) AS isHearted, " +
            "(CASE WHEN EXISTS (SELECT 1 FROM bookmark b WHERE b.postId = p.postId AND b.userId = ?) THEN true ELSE false END) AS isBookmarked " +
            "FROM post p JOIN user u ON p.userId = u.userId WHERE p.postId = ?";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userService.getLoggedInUser().getUserId());
            pstmt.setString(2, userService.getLoggedInUser().getUserId());
            pstmt.setString(3, postId);

            try (ResultSet rs = pstmt.executeQuery()) {
                List<ExpandedPost> posts = Utility.convertResultSetToExpandedPostList(rs, conn);
                if (posts.isEmpty()) {
                    mv.addObject("isNoContent", true);
                }
                mv.addObject("posts", posts);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            String message = URLEncoder.encode("Failed to load post. Please try again.", StandardCharsets.UTF_8);
            return new ModelAndView("redirect:/?error=" + message);
        }

        mv.addObject("errorMessage", error);
        return mv;
    }
    

    /**
     * Handles comments added on posts.
     * See comments on webpage function to see how path variables work here.
     * This function handles form posts.
     * See comments in HomeController.java regarding form submissions.
     */
    @PostMapping("/{postId}/comment")
    public String postComment(@PathVariable("postId") String postId,
            @RequestParam(name = "comment") String comment) {
        System.out.println("The user is attempting add a comment:");
        System.out.println("\tpostId: " + postId);
        System.out.println("\tcomment: " + comment);
        
        final String sql = "insert into comment (postId, userId, content, commentDate) values (?, ?, ?, NOW())";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, postId);
            pstmt.setString(2, userService.getLoggedInUser().getUserId());
            pstmt.setString(3, comment);
           // pstmt.setString(4, java.time.LocalDateTime.now().toString());

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                // Redirect the user if the comment adding is a success.
                return "redirect:/post/" + postId;
            }

        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
        }
        // Redirect the user if the comment adding is a success.
        // return "redirect:/post/" + postId;

        // Redirect the user with an error message if there was an error.
        String message = URLEncoder.encode("Failed to post the comment. Please try again.",
                StandardCharsets.UTF_8);
        return "redirect:/post/" + postId + "?error=" + message;
    }

    /**
     * Handles likes added on posts.
     * See comments on webpage function to see how path variables work here.
     * See comments in PeopleController.java in followUnfollowUser function regarding 
     * get type form submissions and how path variables work.
     */
    @GetMapping("/{postId}/heart/{isAdd}")
    public String addOrRemoveHeart(@PathVariable("postId") String postId,
            @PathVariable("isAdd") Boolean isAdd) {
        System.out.println("The user is attempting add or remove a heart:");
        System.out.println("\tpostId: " + postId);
        System.out.println("\tisAdd: " + isAdd);

        if (isAdd) {
            final String sql = "insert into heart (postId, userId) values (?, ?)";
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, postId);
                pstmt.setString(2, userService.getLoggedInUser().getUserId());

                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    // Redirect the user if the heart adding is a success.
                    return "redirect:/post/" + postId;
                }

            } catch (SQLException e) {
                System.err.println("SQL Error: " + e.getMessage());
            }
        } else {
            final String sql = "delete from heart where postId = ? and userId = ?";
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, postId);
                pstmt.setString(2, userService.getLoggedInUser().getUserId());

                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    // Redirect the user if the heart removing is a success.
                    return "redirect:/post/" + postId;
                }

            } catch (SQLException e) {
                System.err.println("SQL Error: " + e.getMessage());
            }
        }
        // Redirect the user if the comment adding is a success.
        // return "redirect:/post/" + postId;

        // Redirect the user with an error message if there was an error.
        String message = URLEncoder.encode("Failed to (un)like the post. Please try again.",
                StandardCharsets.UTF_8);
        return "redirect:/post/" + postId + "?error=" + message;
    }

    /**
     * Handles bookmarking posts.
     * See comments on webpage function to see how path variables work here.
     * See comments in PeopleController.java in followUnfollowUser function regarding 
     * get type form submissions.
     */
    @GetMapping("/{postId}/bookmark/{isAdd}")
    public String addOrRemoveBookmark(@PathVariable("postId") String postId,
            @PathVariable("isAdd") Boolean isAdd) {
        System.out.println("The user is attempting add or remove a bookmark:");
        System.out.println("\tpostId: " + postId);
        System.out.println("\tisAdd: " + isAdd);

        // Redirect the user if the comment adding is a success.
        // return "redirect:/post/" + postId;
        if (isAdd){
            final String sql = "insert into bookmark (postId, userId) values (?, ?)";
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, postId);
                pstmt.setString(2, userService.getLoggedInUser().getUserId());

                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    // Redirect the user if the bookmark adding is a success.
                    return "redirect:/post/" + postId;
                }

            } catch (SQLException e) {
                System.err.println("SQL Error: " + e.getMessage());
            }
        } else {
            final String sql = "delete from bookmark where postId = ? and userId = ?";
            try (Connection conn = dataSource.getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, postId);
                pstmt.setString(2, userService.getLoggedInUser().getUserId());

                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    // Redirect the user if the bookmark removing is a success.
                    return "redirect:/post/" + postId;
                }

            } catch (SQLException e) {
                System.err.println("SQL Error: " + e.getMessage());
            }
        }
        
        // Redirect the user with an error message if there was an error.
        String message = URLEncoder.encode("Failed to (un)bookmark the post. Please try again.",
                StandardCharsets.UTF_8);
        return "redirect:/post/" + postId + "?error=" + message;
    }

}
