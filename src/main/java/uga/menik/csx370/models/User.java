/**
Copyright (c) 2024 Sami Menik, PhD. All rights reserved.

This is a project developed by Dr. Menik to give the students an opportunity to apply database concepts learned in the class in a real world project. Permission is granted to host a running version of this software and to use images or videos of this work solely for the purpose of demonstrating the work to potential employers. Any form of reproduction, distribution, or transmission of the software's source code, in part or whole, without the prior written consent of the copyright owner, is strictly prohibited.
*/
package uga.menik.csx370.models;

/**
 * Represents a user of the ActorDle game platform.
 *
 * Updated to match the actordle database schema:
 *   table: user
 *   cols:  user_id, username, email, password_hash, salt, created_at
 *
 * firstName/lastName removed - our schema uses a single username field.
 * email added to support the registration flow.
 * profileImagePath kept and still generated from userId hash so
 * any templates using it continue to work unchanged.
 */
public class User {

    /**
     * Unique identifier for the user (maps to user_id in the database).
     */
    private final String userId;

    /**
     * The user's chosen display name (maps to username in the database).
     */
    private final String username;

    /**
     * The user's email address (maps to email in the database).
     */
    private final String email;

    /**
     * Path of the profile image file for the user.
     * Auto-generated from userId - not stored in the database.
     */
    private final String profileImagePath;

    /**
     * Full constructor used when all fields are available (e.g. profile page).
     *
     * @param userId           the unique identifier of the user
     * @param username         the display name of the user
     * @param email            the email address of the user
     * @param profileImagePath the path of the profile image file for the user
     */
    public User(String userId, String username, String email, String profileImagePath) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.profileImagePath = profileImagePath;
    }

    /**
     * Convenience constructor - auto-generates the profile image path.
     * Used by UserService.authenticate() where email is not needed at runtime.
     *
     * @param userId   the unique identifier of the user
     * @param username the display name of the user
     * @param email    the email address of the user
     */
    public User(String userId, String username, String email) {
        this(userId, username, email, getAvatarPath(userId));
    }

    /**
     * Minimal constructor for cases where only userId and username are known
     * (e.g. session restore). Email defaults to empty string.
     *
     * @param userId   the unique identifier of the user
     * @param username the display name of the user
     */
    public User(String userId, String username) {
        this(userId, username, "", getAvatarPath(userId));
    }

    /**
     * Given a userId, generates a deterministic avatar path from the pool
     * of 20 pre-existing avatar images.
     */
    private static String getAvatarPath(String userId) {
        int fileNo = (Math.abs(userId.hashCode()) % 20) + 1;
        String avatarFileName = String.format("avatar_%d.png", fileNo);
        return "/avatars/" + avatarFileName;
    }

    /**
     * Returns the user ID.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Returns the username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the email address.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Returns the path of the profile image file for the user.
     */
    public String getProfileImagePath() {
        return profileImagePath;
    }

    /**
     * Returns the username as the display name.
     * Kept for any templates that previously called getFirstName().
     */
    public String getFirstName() {
        return username;
    }

    /**
     * Returns an empty string.
     * Kept for backwards compatibility with templates that called getLastName().
     */
    public String getLastName() {
        return "";
    }

}