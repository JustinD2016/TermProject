package uga.menik.csx370.utility;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import uga.menik.csx370.models.Comment;
import uga.menik.csx370.models.ExpandedPost;
import uga.menik.csx370.models.FollowableUser;
import uga.menik.csx370.models.Post;
import uga.menik.csx370.models.User;
import uga.menik.csx370.models.Poll;
import uga.menik.csx370.models.PollOption;

public class Utility {

    public static List<FollowableUser> createSampleFollowableUserList() {
        List<FollowableUser> followableUsers = new ArrayList<>();
        followableUsers.add(new FollowableUser("1", "John", "Doe",
                true, "Mar 07, 2024, 10:54 PM"));
        followableUsers.add(new FollowableUser("2", "Jane", "Doe",
                false, "Mar 05, 2024, 11:00 AM"));
        followableUsers.add(new FollowableUser("3", "Alice", "Smith",
                true, "Mar 06, 2024, 09:30 AM"));
        followableUsers.add(new FollowableUser("4", "Bob", "Brown",
                false, "Mar 02, 2024, 08:15 PM"));
        return followableUsers;
    }

    public static List<Post> createSamplePostsListWithoutComments() {
        User user1 = new User("1", "John", "Doe");
        User user2 = new User("2", "Jane", "Doe");
        User user3 = new User("3", "Alice", "Smith");
        //User user4 = new User("4", "Bob", "Brown");
        //User user5 = new User("5", "Charlie", "Green");
        List<Post> postsWithoutComments = new ArrayList<>();
        postsWithoutComments.add(new Post("1", "Exploring Spring Boot features",
                "Mar 07, 2024, 10:54 PM", user1, 10, 4, false, false));
        postsWithoutComments.add(new Post("2", "Introduction to Microservices",
                "Mar 08, 2024, 11:00 AM", user2, 15, 6, true, true));
        postsWithoutComments.add(new Post("3", "Basics of Reactive Programming",
                "Mar 09, 2024, 09:30 AM", user3, 20, 3, true, false));
        return postsWithoutComments;
    }

    public static List<ExpandedPost> createSampleExpandedPostWithComments() {
        User user1 = new User("1", "John", "Doe");
        User user2 = new User("2", "Jane", "Doe");
        //User user3 = new User("3", "Alice", "Smith");
        User user4 = new User("4", "Bob", "Brown");
        User user5 = new User("5", "Charlie", "Green");
        List<Comment> commentsForPost = new ArrayList<>();

        commentsForPost.add(new Comment("1", "Great insights, thanks for sharing!", 
            "Mar 07, 2024, 10:54 PM", user2));
        commentsForPost.add(new Comment("2", "I'm looking forward to trying this out.", 
            "Mar 08, 2024, 11:00 AM", user4));
        commentsForPost.add(new Comment("3", "Can you provide more examples in your next post?", 
            "Mar 09, 2024, 09:30 AM", user5));
        ExpandedPost postWithComments = new ExpandedPost("4", "Advanced Techniques " + 
            "in Spring Security", "Mar 10, 2024, 08:15 PM", user1, 25, 
            commentsForPost.size(), false, true, commentsForPost);
        return List.of(postWithComments);
    }

    public static List<Post> convertResultSetToPostList(java.sql.ResultSet rs) throws java.sql.SQLException {
        List<Post> posts = new ArrayList<>();
        while (rs.next()) {
            String postId = rs.getString("postId");
            String userId = rs.getString("userId");
            String firstName = rs.getString("firstName");
            String lastName = rs.getString("lastName");
            String content = rs.getString("content");
            String postDate = rs.getString("postDate");
            int heartsCount = rs.getInt("heartsCount");
            int commentsCount = rs.getInt("commentsCount");
            boolean isHearted = rs.getInt("isHearted") > 0;
            boolean isBookmarked = rs.getInt("isBookmarked") > 0;

            User user = new User(userId, firstName, lastName);
            Post post = new Post(postId, content, postDate, user, heartsCount, commentsCount, isHearted, isBookmarked);
            posts.add(post);
        }
        return posts;
    }
    public static List<ExpandedPost> convertResultSetToExpandedPostList(java.sql.ResultSet rs, java.sql.Connection conn) throws java.sql.SQLException {
        List<ExpandedPost> posts = new ArrayList<>();
        while (rs.next()) {
            String postId = rs.getString("postId");
            String userId = rs.getString("userId");
            String firstName = rs.getString("firstName");
            String lastName = rs.getString("lastName");
            String content = rs.getString("content");
            String postDate = rs.getString("postDate");
            int heartsCount = rs.getInt("heartsCount");
            int commentsCount = rs.getInt("commentsCount");
            boolean isHearted = rs.getBoolean("isHearted");
            boolean isBookmarked = rs.getBoolean("isBookmarked");

            User user = new User(userId, firstName, lastName);

            // Second query to get comments for this post
            List<Comment> comments = new ArrayList<>();
            final String commentSql = "SELECT c.commentId, c.content, DATE_FORMAT(c.commentDate, '%b %d, %Y, %h:%i %p') AS commentDate, c.userId, u.firstName, u.lastName " +
                    "FROM comment c JOIN user u ON c.userId = u.userId " +
                    "WHERE c.postId = ? ORDER BY c.commentDate ASC";

            try (PreparedStatement commentStmt = conn.prepareStatement(commentSql)) {
                commentStmt.setString(1, postId);
                try (java.sql.ResultSet commentRs = commentStmt.executeQuery()) {
                    while (commentRs.next()) {
                        String commentId = commentRs.getString("commentId");
                        String commentContent = commentRs.getString("content");
                        String commentDate = commentRs.getString("commentDate");
                        String commentUserId = commentRs.getString("userId");
                        String commentFirstName = commentRs.getString("firstName");
                        String commentLastName = commentRs.getString("lastName");

                        User commentUser = new User(commentUserId, commentFirstName, commentLastName);
                        comments.add(new Comment(commentId, commentContent, commentDate, commentUser));
                    }
                }
            }

            ExpandedPost post = new ExpandedPost(postId, content, postDate, user, heartsCount, commentsCount, isHearted, isBookmarked, comments);
            posts.add(post);
        }
    return posts;
}

    public static List<Poll> convertResultSetToPollList(java.sql.ResultSet rs) throws java.sql.SQLException {
            // polls stores all polls made from ResultSet
            List<Poll> polls = new ArrayList<>();

            // Tracks all polls being converted (cPollId is current poll id)
            String cPollId = null;
            Poll cPoll = null;

            // Contains the options for the current poll
            List<PollOption> cOptions = null; 

            while (rs.next()) {
                String pollId = rs.getString("pollId");

                // If the poll is the first or new id, create new poll object
                if (cPollId == null || !cPollId.equals(pollId)) {
                    // Update current pollId
                    cPollId = pollId;

                    // get attribute values
                    String userId = rs.getString("userId");
                    String firstName = rs.getString("firstName");
                    String lastName = rs.getString("lastName");
                    User user = new User(userId, firstName, lastName);

                    // create list to store current poll's options
                    cOptions = new ArrayList<>();

                    // create poll with attributes
                    String question = rs.getString("question");
                    String pollDate = rs.getString("pollDate");
                    cPoll = new Poll(pollId, question, pollDate, user, cOptions);

                    // add current poll to polls
                    polls.add(cPoll);
                }

                // get attributes of the options for the current poll
                String optionId = rs.getString("optionId");
                String optionText = rs.getString("optionText");
                int voteCount = rs.getInt("voteCount");
                boolean userVote = rs.getInt("userVote") > 0; // these come from COUNT(*) so > 0 converts them to boolean
                boolean hasVoted = rs.getInt("hasVoted") > 0; // I had trouble with this one and found this as a solution
                PollOption option = new PollOption (optionId, optionText, voteCount, userVote, hasVoted);

                // add options to the current poll's options
                cOptions.add(option);

            }
        return polls;
    }

}
