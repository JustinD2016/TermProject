package uga.menik.csx370.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import uga.menik.csx370.models.User;

/**
 * This is a service class that enables user related functions.
 * The class interacts with the database through a dataSource instance.
 * See authenticate and registerUser functions for examples.
 * This service object is special. Its lifetime is limited to a user session.
 * Usual services generally have application lifetime.
 *
 * Updated to match the actordle database schema:
 *   table:  user
 *   cols:   user_id, username, email, password_hash, salt, created_at
 *
 * On registration a matching user_stats row is also inserted.
 */
@Service
@SessionScope
public class UserService {

    // dataSource enables talking to the database.
    private final DataSource dataSource;
    // passwordEncoder is used for password security.
    private final BCryptPasswordEncoder passwordEncoder;
    // Holds the currently logged-in user for this session.
    private User loggedInUser = null;

    /**
     * See AuthInterceptor notes regarding dependency injection and
     * inversion of control.
     */
    @Autowired
    public UserService(DataSource dataSource) {
        this.dataSource = dataSource;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * Authenticates a user given username and password.
     * Stores the user object in session scope on success.
     * Returns true if authentication is successful, false otherwise.
     *
     * Column mapping (actordle schema):
     *   user_id       -> User.userId
     *   first_name    -> User.firstName  (derived from username for display)
     *   last_name     -> User.lastName
     *   password_hash -> compared via BCrypt
     */
    public boolean authenticate(String username, String password) throws SQLException {
        final String sql = "SELECT user_id, username, password_hash FROM user WHERE username = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                // This while loop runs at most once since username is unique.
                while (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    boolean isPassMatch = passwordEncoder.matches(password, storedHash);

                    if (isPassMatch) {
                        String userId = rs.getString("user_id");
                        // Our schema stores a single username rather than
                        // separate first/last name — pass username for both
                        // so existing User model constructor still works.
                        loggedInUser = new User(userId, username, "");
                    }
                    return isPassMatch;
                }
            }
        }
        return false;
    }

    /**
     * Logs out the current user.
     */
    public void unAuthenticate() {
        loggedInUser = null;
    }

    /**
     * Checks if a user is currently authenticated.
     */
    public boolean isAuthenticated() {
        return loggedInUser != null;
    }

    /**
     * Retrieves the currently logged-in user.
     */
    public User getLoggedInUser() {
        return loggedInUser;
    }

    /**
     * Registers a new user with the given details.
     * Returns true if registration is successful.
     *
     * Also inserts a default user_stats row for the new user so that
     * stats queries never return null for a registered user.
     *
     * Throws SQLException if the username or email already exists
     * (unique constraint violation) — caller should handle this.
     */
    public boolean registerUser(String username, String password, String email)
            throws SQLException {

        // BCrypt generates its own salt internally and embeds it in the hash.
        // We store the full hash in password_hash and the extracted salt in salt.
        String passwordHash = passwordEncoder.encode(password);
        // Extract the salt prefix from the BCrypt hash (first 29 chars).
        String salt = passwordHash.substring(0, 29);

        final String registerSql =
            "INSERT INTO user (username, email, password_hash, salt) " +
            "VALUES (?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement registerStmt = conn.prepareStatement(
                     registerSql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            registerStmt.setString(1, username);
            registerStmt.setString(2, email);
            registerStmt.setString(3, passwordHash);
            registerStmt.setString(4, salt);

            int rowsAffected = registerStmt.executeUpdate();

            if (rowsAffected > 0) {
                // Get the auto-generated user_id so we can insert user_stats.
                try (ResultSet keys = registerStmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        int newUserId = keys.getInt(1);

                        // Insert a blank stats row — all counters default to 0.
                        final String statsSql =
                            "INSERT INTO user_stats (user_id) VALUES (?)";

                        try (PreparedStatement statsStmt =
                                conn.prepareStatement(statsSql)) {
                            statsStmt.setInt(1, newUserId);
                            statsStmt.executeUpdate();
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

}