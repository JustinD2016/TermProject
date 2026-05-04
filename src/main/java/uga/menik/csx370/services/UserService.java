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
 * This service object is special. Its lifetime is limited to a user session.
 *
 * Matches the actordle database schema:
 *   table: user
 *   cols:  user_id, username, email, password_hash, salt, created_at
 *
 * On registration a matching user_stats row is also inserted.
 */
@Service
@SessionScope
public class UserService {

    private final DataSource dataSource;
    private final BCryptPasswordEncoder passwordEncoder;
    private User loggedInUser = null;

    @Autowired
    public UserService(DataSource dataSource) {
        this.dataSource = dataSource;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * Authenticates a user given username and password.
     * Stores the user object in session scope on success.
     * Returns true if authentication is successful, false otherwise.
     */
    public boolean authenticate(String username, String password) throws SQLException {
        final String authenticateSql = "SELECT user_id, username, password_hash FROM user WHERE username = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(authenticateSql)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                // Runs at most once since username is unique.
                while (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    boolean isPassMatch = passwordEncoder.matches(password, storedHash);

                    if (isPassMatch) {
                        String userId = rs.getString("user_id");
                        loggedInUser = new User(userId, username);
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
     * Also inserts a default user_stats row for the new user.
     * Throws SQLException on duplicate username or email.
     */
    public boolean registerUser(String username, String password, String email)
            throws SQLException {

        // BCrypt embeds its own salt in the hash string.
        // We store the full hash and extract the 29-char salt prefix separately.
        String passwordHash = passwordEncoder.encode(password);
        String salt = passwordHash.substring(0, 29);

        final String registerSql =
            "INSERT INTO user (username, email, password_hash, salt) VALUES (?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement registerStmt = conn.prepareStatement(
                     registerSql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            registerStmt.setString(1, username);
            registerStmt.setString(2, email);
            registerStmt.setString(3, passwordHash);
            registerStmt.setString(4, salt);

            int rowsAffected = registerStmt.executeUpdate();

            if (rowsAffected > 0) {
                // Insert a blank stats row using the new user_id.
                try (ResultSet keys = registerStmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        int newUserId = keys.getInt(1);

                        final String statsSql = "INSERT INTO user_stats (user_id) VALUES (?)";
                        try (PreparedStatement statsStmt = conn.prepareStatement(statsSql)) {
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