package uga.menik.csx370.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uga.menik.csx370.models.FollowableUser;

/**
 * This service contains people/follow related functions.
 *
 * Updated to match the actordle database schema:
 *   - user table:   user_id, username
 *   - follow table: follower_id, followee_id  (Twitter-style, one-way)
 */
@Service
public class PeopleService {

    private final DataSource dataSource;

    @Autowired
    private UserService userService;

    @Autowired
    public PeopleService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Returns all users that the logged-in user is NOT already following,
     * excluding themselves. Used to populate the "find people" list.
     *
     * @param userIdToExclude the logged-in user's ID
     */
    public List<FollowableUser> getFollowableUsers(String userIdToExclude) {
        List<FollowableUser> users = new ArrayList<>();

        final String sql =
            "SELECT u.user_id, u.username, " +
            "       COALESCE(DATE_FORMAT(gs.last_played, '%b %d, %Y'), 'No games yet') AS lastActivity " +
            "FROM user u " +
            "LEFT JOIN ( " +
            "    SELECT user_id, MAX(played_at) AS last_played " +
            "    FROM game_session " +
            "    GROUP BY user_id " +
            ") gs ON gs.user_id = u.user_id " +
            "WHERE u.user_id != ? " +
            "  AND u.user_id NOT IN ( " +
            "      SELECT followee_id FROM follow WHERE follower_id = ? " +
            "  )";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            int loggedInId = Integer.parseInt(userService.getLoggedInUser().getUserId());
            pstmt.setInt(1, Integer.parseInt(userIdToExclude));
            pstmt.setInt(2, loggedInId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String userId       = rs.getString("user_id");
                    String username     = rs.getString("username");
                    String lastActivity = rs.getString("lastActivity");

                    // isFollowed = false — not yet following these users
                    users.add(new FollowableUser(userId, username, "", false, lastActivity));
                }
            }

        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            e.printStackTrace();
        }

        return users;
    }

    /**
     * Returns all users the logged-in user follows, along with
     * their result on today's daily game if they have played it.
     *
     * Used by the follow list page (Interface 3).
     *
     * @param loggedInUserId the logged-in user's ID
     */
    public List<FollowableUser> getFollowingWithGameResults(String loggedInUserId) {
        List<FollowableUser> following = new ArrayList<>();

        final String sql =
            "SELECT u.user_id, u.username, " +
            "       gs.solved, gs.guesses_used, " +
            "       COALESCE(DATE_FORMAT(gs.played_at, '%b %d, %Y, %h:%i %p'), 'Not played today') AS lastActivity " +
            "FROM follow f " +
            "JOIN user u ON u.user_id = f.followee_id " +
            "LEFT JOIN game_session gs ON gs.user_id = u.user_id " +
            "    AND gs.game_id = (SELECT game_id FROM daily_game WHERE game_date = CURDATE()) " +
            "WHERE f.follower_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, Integer.parseInt(loggedInUserId));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String userId       = rs.getString("user_id");
                    String username     = rs.getString("username");
                    String lastActivity = rs.getString("lastActivity");
                    boolean solvedToday = rs.getBoolean("solved");

                    // Re-use isFollowed = true since these are all followed users
                    following.add(new FollowableUser(userId, username, "", solvedToday, lastActivity));
                }
            }

        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            e.printStackTrace();
        }

        return following;
    }

    /**
     * Follows a user. Inserts a row into the follow table.
     * Returns true if successful.
     *
     * @param followeeId the user_id of the user to follow
     */
    public boolean followUser(String followeeId) {
        final String sql =
            "INSERT IGNORE INTO follow (follower_id, followee_id) VALUES (?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, Integer.parseInt(userService.getLoggedInUser().getUserId()));
            pstmt.setInt(2, Integer.parseInt(followeeId));
            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Unfollows a user. Deletes the row from the follow table.
     * Returns true if successful.
     *
     * @param followeeId the user_id of the user to unfollow
     */
    public boolean unfollowUser(String followeeId) {
        final String sql =
            "DELETE FROM follow WHERE follower_id = ? AND followee_id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, Integer.parseInt(userService.getLoggedInUser().getUserId()));
            pstmt.setInt(2, Integer.parseInt(followeeId));
            int rows = pstmt.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Checks whether the logged-in user is following a given user.
     *
     * @param followeeId the user_id to check
     */
    public boolean isFollowing(String followeeId) {
        final String sql =
            "SELECT 1 FROM follow WHERE follower_id = ? AND followee_id = ? LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, Integer.parseInt(userService.getLoggedInUser().getUserId()));
            pstmt.setInt(2, Integer.parseInt(followeeId));

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            e.printStackTrace();
            return false;
        }
    }

}