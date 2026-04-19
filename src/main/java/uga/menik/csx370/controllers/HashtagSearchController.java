package uga.menik.csx370.controllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import uga.menik.csx370.models.ExpandedPost;
import uga.menik.csx370.utility.Utility;
import uga.menik.csx370.services.UserService;

@Controller
@RequestMapping("/hashtagsearch")
public class HashtagSearchController {

    private final UserService userService;
    private final DataSource dataSource;

    public HashtagSearchController(UserService userService, DataSource dataSource) {
        this.userService = userService;
        this.dataSource = dataSource;
    }

    @GetMapping
    public ModelAndView webpage(@RequestParam(name = "hashtags") String hashtags) {
        System.out.println("User is searching: " + hashtags);

        ModelAndView mv = new ModelAndView("posts_page");

        List<String> hashtagList = extractHashtags(hashtags);

        if (hashtagList.isEmpty()) {
            mv.addObject("isNoContent", true);
            mv.addObject("errorMessage", "Please enter at least one hashtag.");
            return mv;
        }

        try (Connection conn = dataSource.getConnection()) {

            // Ensure tables exist
            initializeHashtagTables(conn);

            String placeholders = String.join(",", java.util.Collections.nCopies(hashtagList.size(), "?"));

            final String sql =
                "SELECT " +
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
                "FROM post p " +
                "JOIN user u ON p.userId = u.userId " +
                "JOIN post_hashtag ph ON p.postId = ph.postId " +
                "JOIN hashtag ht ON ph.hashtagId = ht.hashtagId " +
                "WHERE ht.tag IN (" + placeholders + ") " +
                "GROUP BY p.postId, p.userId, u.firstName, u.lastName, p.content, p.postDate " +
                "HAVING COUNT(DISTINCT ht.tag) = ? " +
                "ORDER BY p.postDate DESC";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

                int index = 1;

                int userId = Integer.parseInt(userService.getLoggedInUser().getUserId());

                // for isHearted + isBookmarked
                pstmt.setInt(index++, userId);
                pstmt.setInt(index++, userId);

                // hashtags
                for (String tag : hashtagList) {
                    pstmt.setString(index++, tag);
                }

                // count match (ALL hashtags)
                pstmt.setInt(index, hashtagList.size());

                try (ResultSet rs = pstmt.executeQuery()) {

                    List<ExpandedPost> posts = Utility.convertResultSetToExpandedPostList(rs, conn);

                    if (posts.isEmpty()) {
                        mv.addObject("isNoContent", true);
                    }

                    mv.addObject("posts", posts);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            mv.addObject("errorMessage", "Error searching hashtags.");
            mv.addObject("isNoContent", true);
        }

        return mv;
    }

    /**
     * Extract hashtags like #hello #world -> ["hello", "world"]
     */
    private List<String> extractHashtags(String text) {
        Set<String> tags = new LinkedHashSet<>();

        Pattern pattern = Pattern.compile("#(\\w+)");
        Matcher matcher = pattern.matcher(text.toLowerCase());

        while (matcher.find()) {
            tags.add(matcher.group(1).trim());
        }

        return new ArrayList<>(tags);
    }

    /**
     * Create tables if they don't exist
     */
    private void initializeHashtagTables(Connection conn) throws SQLException {

        final String createHashtag =
            "CREATE TABLE IF NOT EXISTS hashtag (" +
            "hashtagId INT AUTO_INCREMENT, " +
            "tag VARCHAR(255) NOT NULL, " +
            "PRIMARY KEY (hashtagId), " +
            "UNIQUE (tag))";

        final String createPostHashtag =
            "CREATE TABLE IF NOT EXISTS post_hashtag (" +
            "postId INT NOT NULL, " +
            "hashtagId INT NOT NULL, " +
            "PRIMARY KEY (postId, hashtagId), " +
            "FOREIGN KEY (postId) REFERENCES post(postId) ON DELETE CASCADE, " +
            "FOREIGN KEY (hashtagId) REFERENCES hashtag(hashtagId) ON DELETE CASCADE)";

        try (PreparedStatement p1 = conn.prepareStatement(createHashtag);
             PreparedStatement p2 = conn.prepareStatement(createPostHashtag)) {

            p1.execute();
            p2.execute();
        }
    }
}