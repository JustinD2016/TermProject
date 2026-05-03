package uga.menik.csx370.utility;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Component
public class DatabaseSeeder {

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void seed(){
        // Insert a new daily game with a random actor if one hasn't been picked for today.
        final String sql = "INSERT IGNORE INTO daily_game (game_date, actor_id) " +
                           "SELECT CURDATE(), actor_id FROM actor ORDER BY RAND() LIMIT 1";

        try (Connection conn = dataSource.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
                //Utility.seedAll(conn);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
    }
}
