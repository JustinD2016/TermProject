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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import uga.menik.csx370.models.Post;
import uga.menik.csx370.utility.Utility;
import uga.menik.csx370.services.UserService;


/**
 * This controller handles the home page and some of it's sub URLs.
 */
@Controller
@RequestMapping
public class HomeController {

    private final DataSource dataSource;
    private final UserService userService;
    public HomeController(DataSource dataSource, UserService userService) {
        this.dataSource = dataSource;
        this.userService = userService;
    }
    /**
     * This is the specific function that handles the root URL itself.
     * 
     * Note that this accepts a URL parameter called error.
     * The value to this parameter can be shown to the user as an error message.
     * See notes in HashtagSearchController.java regarding URL parameters.
     */
    @GetMapping
    public ModelAndView webpage(@RequestParam(name = "error", required = false) String error) {
        // See notes on ModelAndView in BookmarksController.java.
        ModelAndView mv = new ModelAndView("home_page");

        // Following line populates sample data.
        // You should replace it with actual data from the database.
         final String sql = "SELECT " +
            "p.postId, " +
            "p.userId, " +
            "u.firstName, " +
            "u.lastName, " +
            "p.content, " +
            "DATE_FORMAT(p.postDate, '%b %d, %Y, %h:%i %p') AS postDate, " +
            "(SELECT COUNT(*) FROM heart h WHERE h.postId = p.postId) AS heartsCount, " +
            "(SELECT COUNT(*) FROM comment c WHERE c.postId = p.postId) AS commentsCount, " +
            "(SELECT COUNT(*) FROM bookmark b WHERE b.postId = p.postId AND b.userId = ?) AS isBookmarked, " +
            "(SELECT COUNT(*) FROM heart h WHERE h.postId = p.postId AND h.userId = ?) AS isHearted " +
            "FROM post p " +
            "JOIN user u ON p.userId = u.userId " +
            "WHERE (p.userID IN (SELECT followerID FROM follow WHERE followeeID = ?) OR p.userId = ?) " +
            "ORDER BY p.postDate DESC";
        // Keaton -- I added OR p.userID on line 71 since users should be able to see their own posts too
        try (Connection conn = dataSource.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {

        String loggedInUserId = userService.getLoggedInUser().getUserId();
        pstmt.setInt(1, Integer.parseInt(loggedInUserId));
        pstmt.setInt(2, Integer.parseInt(loggedInUserId));
        pstmt.setInt(3, Integer.parseInt(loggedInUserId));
        pstmt.setInt(4, Integer.parseInt(loggedInUserId));

        try (ResultSet rs = pstmt.executeQuery()) {
            List<Post> posts = Utility.convertResultSetToPostList(rs);
                if (posts.isEmpty()) {
                    mv.addObject("isNoContent", true);
                }
                mv.addObject("posts", posts);
        }

        } catch (SQLException e) {
            e.printStackTrace();
            String message = URLEncoder.encode("Failed to load posts. Please try again.", StandardCharsets.UTF_8);
            return new ModelAndView("redirect:/?error=" + message);
        }

        String errorMessage = error;
        mv.addObject("errorMessage", errorMessage);

        return mv;
    }

    /**
     * This function handles the /createpost URL.
     * This handles a post request that is going to be a form submission.
     * The form for this can be found in the home page. The form has a
     * input field with name = posttext. Note that the @RequestParam
     * annotation has the same name. This makes it possible to access the value
     * from the input from the form after it is submitted.
     */
    @PostMapping("/createpost")
    public String createPost(@RequestParam(name = "posttext") String postText) {
        if (postText == null || postText.trim().isEmpty()) { // For the requirement "Do not allow creating empty posts".
            String message = URLEncoder.encode("Failed to create poll. Please try again.",
                StandardCharsets.UTF_8);
            return "redirect:/?error=" + message;
        }
        System.out.println("User is creating post: " + postText);

        final String insertPostSql = "INSERT INTO post (userId, content, postDate) VALUES (?, ?, NOW())";

        try (Connection conn = dataSource.getConnection();
            PreparedStatement insertPost = conn.prepareStatement(insertPostSql)) {

            int userId = Integer.parseInt(userService.getLoggedInUser().getUserId());

            insertPost.setInt(1, userId);
            insertPost.setString(2, postText);
            insertPost.executeUpdate();

            try (PreparedStatement createHashtagTable = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS hashtag (" +
                    "hashtagId INT AUTO_INCREMENT, " +
                    "tag VARCHAR(255) NOT NULL, " +
                    "PRIMARY KEY (hashtagId), " +
                    "UNIQUE (tag))");
                PreparedStatement createPostHashtagTable = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS post_hashtag (" +
                    "postId INT NOT NULL, " +
                    "hashtagId INT NOT NULL, " +
                    "PRIMARY KEY (postId, hashtagId), " +
                    "FOREIGN KEY (postId) REFERENCES post(postId) ON DELETE CASCADE, " +
                    "FOREIGN KEY (hashtagId) REFERENCES hashtag(hashtagId) ON DELETE CASCADE)")) {

                createHashtagTable.execute();
                createPostHashtagTable.execute();
            }

            int postId = -1;
            try (PreparedStatement getPostId = conn.prepareStatement(
                    "SELECT MAX(postId) AS id FROM post WHERE userId = ?")) {
                getPostId.setInt(1, userId);
                try (ResultSet rs = getPostId.executeQuery()) {
                    if (rs.next()) {
                        postId = rs.getInt("id");
                    }
                }
            }

            java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("#(\\w+)")
                .matcher(postText.toLowerCase());

            while (m.find()) {
                String tag = m.group(1);

                try (PreparedStatement insertTag = conn.prepareStatement(
                        "INSERT IGNORE INTO hashtag(tag) VALUES (?)")) {
                    insertTag.setString(1, tag);
                    insertTag.executeUpdate();
                }

                int hashtagId = -1;
                try (PreparedStatement getTagId = conn.prepareStatement(
                        "SELECT hashtagId FROM hashtag WHERE tag = ?")) {
                    getTagId.setString(1, tag);
                    try (ResultSet rs = getTagId.executeQuery()) {
                        if (rs.next()) {
                            hashtagId = rs.getInt("hashtagId");
                        }
                    }
                }

                if (postId != -1 && hashtagId != -1) {
                    try (PreparedStatement linkTag = conn.prepareStatement(
                            "INSERT IGNORE INTO post_hashtag(postId, hashtagId) VALUES (?, ?)")) {
                        linkTag.setInt(1, postId);
                        linkTag.setInt(2, hashtagId);
                        linkTag.executeUpdate();
                    }
                }
            }

            return "redirect:/";

        } catch (SQLException e) {
            e.printStackTrace();
        }

        String message = URLEncoder.encode("Failed to create the post. Please try again.",
                StandardCharsets.UTF_8);
        return "redirect:/?error=" + message;
    }

}
