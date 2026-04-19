/**
Copyright (c) 2024 Sami Menik, PhD. All rights reserved.

This is a project developed by Dr. Menik to give the students an opportunity to apply database concepts learned in the class in a real world project. Permission is granted to host a running version of this software and to use images or videos of this work solely for the purpose of demonstrating the work to potential employers. Any form of reproduction, distribution, or transmission of the software's source code, in part or whole, without the prior written consent of the copyright owner, is strictly prohibited.
*/
package uga.menik.csx370.services;

import java.util.List;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uga.menik.csx370.models.FollowableUser;
import uga.menik.csx370.utility.Utility;

/**
 * This service contains people related functions.
 */
@Service
public class PeopleService {
    private final DataSource dataSource;

    @Autowired
    public PeopleService(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    @Autowired
    private UserService userService;

    /**
     * This function should query and return all users that
     * are followable. The list should not contain the user
     * with id userIdToExclude.
     */
    public List<FollowableUser> getFollowableUsers(String userIdToExclude) {
        List<FollowableUser> fol = new ArrayList<>();
        final String sql = "SELECT u.userId, u.firstName, u.lastName, COALESCE(DATE_FORMAT(lastPost.lastPostDate, '%b %d, %Y, %h:%i %p'), 'No posts yet') AS lastPostDate " + 
                            "FROM user u " + 
                            "LEFT JOIN "  + 
                            "(SELECT p.userId, MAX(p.postDate) AS lastPostDate " +
                            "FROM post p " + 
                            "GROUP BY p.userId)  lastPost ON u.userId = lastPost.userId " +
                            "WHERE u.userId != ? AND u.userId NOT IN (SELECT followerId FROM follow WHERE followeeId = ?)";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userIdToExclude);
            pstmt.setString(2, userService.getLoggedInUser().getUserId());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String userId = rs.getString("userId");
                    String firstName = rs.getString("firstName");
                    String lastName = rs.getString("lastName");
                    String lastPostDate = rs.getString("lastPostDate");
                    boolean isFollowed = false;
                    FollowableUser followableUser = new FollowableUser(userId, firstName, lastName,  isFollowed, lastPostDate);
                    fol.add(followableUser);
                }
            }

        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            e.printStackTrace();
            return Utility.createSampleFollowableUserList();
        }

        return fol;
    }
}