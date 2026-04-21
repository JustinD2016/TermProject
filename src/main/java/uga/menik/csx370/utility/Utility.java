package uga.menik.csx370.utility;

import org.springframework.security.crypto.bcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;



/**
 * Utility class for ActorDle.
 * Contains static methods to seed sample data into the database
 * and any shared helper logic.
 */
public class Utility {

    
    // ----------------------------------------------------------------
    // SEED METHODS
    // ----------------------------------------------------------------

    /**
     * Inserts two sample users (testuser1, testuser2) and their stat rows.
     * Safe to call multiple times — uses INSERT IGNORE.
     */
    public static void seedUsers(Connection conn) throws SQLException {
        final String insertUser =
            "INSERT IGNORE INTO user (username, email, password_hash, salt) VALUES (?, ?, ?, ?)";

        String[][] users = {
            { "testuser1", "user1@example.com" },
            { "testuser2", "user2@example.com" }
        };

        try (PreparedStatement ps = conn.prepareStatement(
                insertUser, PreparedStatement.RETURN_GENERATED_KEYS)) {

            for (String[] u : users) {
                String salt     = BCrypt.gensalt(12);
                String hash     = BCrypt.hashpw(u[0] + "_password", salt); // username + "_password" as default
                ps.setString(1, u[0]);
                ps.setString(2, u[1]);
                ps.setString(3, hash);
                ps.setString(4, salt);
                ps.addBatch();
            }
            ps.executeBatch();
        }

        // Insert user_stats rows for any user that doesn't have one yet
        final String insertStats =
            "INSERT IGNORE INTO user_stats (user_id) " +
            "SELECT user_id FROM user WHERE username IN ('testuser1', 'testuser2')";

        try (PreparedStatement ps = conn.prepareStatement(insertStats)) {
            ps.executeUpdate();
        }
    }

    /**
     * Makes testuser1 follow testuser2.
     * Safe to call multiple times — uses INSERT IGNORE.
     */
    public static void seedFollow(Connection conn) throws SQLException {
        final String sql =
            "INSERT IGNORE INTO follow (follower_id, followee_id) " +
            "SELECT f.user_id, e.user_id " +
            "FROM user f, user e " +
            "WHERE f.username = 'testuser1' AND e.username = 'testuser2'";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    /**
     * Inserts a small set of sample actors.
     * Safe to call multiple times — uses INSERT IGNORE.
     */
    public static void seedActors(Connection conn) throws SQLException {
        final String sql =
            "INSERT IGNORE INTO actor " +
            "(actor_id, first_name, last_name, birth_year, death_year, primary_profession) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

        Object[][] actors = {
            { "nm0000102", "Bryan",   "Cranston",  1956, null, "actor" },
            { "nm0000151", "Jack",    "Nicholson", 1937, null, "actor" },
            { "nm0000093", "Anthony", "Hopkins",   1937, null, "actor" },
        };

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Object[] a : actors) {
                ps.setString(1, (String)  a[0]);
                ps.setString(2, (String)  a[1]);
                ps.setString(3, (String)  a[2]);
                ps.setObject(4,            a[3]); // birth_year (nullable)
                ps.setObject(5,            a[4]); // death_year (nullable)
                ps.setString(6, (String)  a[5]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Inserts a small set of sample titles.
     * Safe to call multiple times — uses INSERT IGNORE.
     */
    public static void seedTitles(Connection conn) throws SQLException {
        final String sql =
            "INSERT IGNORE INTO title (title_id, title_type, primary_title, start_year, genres) " +
            "VALUES (?, ?, ?, ?, ?)";

        Object[][] titles = {
            { "tt0903747", "tvSeries", "Breaking Bad",             2008, "Crime,Drama,Thriller" },
            { "tt0107290", "movie",    "Jurassic Park",             1993, "Action,Adventure,Sci-Fi" },
            { "tt0111161", "movie",    "The Shawshank Redemption",  1994, "Drama" },
            { "tt0068646", "movie",    "The Godfather",             1972, "Crime,Drama" },
        };

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Object[] t : titles) {
                ps.setString(1, (String) t[0]);
                ps.setString(2, (String) t[1]);
                ps.setString(3, (String) t[2]);
                ps.setObject(4,           t[3]);
                ps.setString(5, (String) t[4]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Links actors to titles in the actor_title junction table.
     * Safe to call multiple times — uses INSERT IGNORE.
     */
    public static void seedActorTitles(Connection conn) throws SQLException {
        final String sql =
            "INSERT IGNORE INTO actor_title (actor_id, title_id) VALUES (?, ?)";

        String[][] links = {
            { "nm0000102", "tt0903747" }, // Bryan Cranston -> Breaking Bad
            { "nm0000151", "tt0068646" }, // Jack Nicholson -> The Godfather
        };

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String[] link : links) {
                ps.setString(1, link[0]);
                ps.setString(2, link[1]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Inserts today's daily game (Bryan Cranston) if one doesn't exist yet.
     */
    public static void seedDailyGame(Connection conn) throws SQLException {
        final String sql =
            "INSERT IGNORE INTO daily_game (game_date, actor_id) " +
            "VALUES (CURDATE(), 'nm0000102')";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    /**
     * Calls all seed methods in dependency order.
     * Safe to call on every startup — all inserts use INSERT IGNORE.
     */
    public static void seedAll(Connection conn) throws SQLException {
        seedActors(conn);
        seedTitles(conn);
        seedActorTitles(conn);
        seedUsers(conn);
        seedFollow(conn);
        seedDailyGame(conn);
    }

    // ----------------------------------------------------------------
    // PASSWORD HELPERS
    // ----------------------------------------------------------------

    /**
     * Hashes a plain-text password using BCrypt.
     * The salt is embedded in the returned hash — no need to store separately,
     * but we keep the salt column for compatibility with the schema.
     */
    public static String hashPassword(String plainText) {
        return BCrypt.hashpw(plainText, BCrypt.gensalt(12));
    }

    /**
     * Verifies a plain-text password against a stored BCrypt hash.
     */
    public static boolean verifyPassword(String plainText, String storedHash) {
        return BCrypt.checkpw(plainText, storedHash);
    }
}