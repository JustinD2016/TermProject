package uga.menik.csx370.utility;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class DatabaseSeeder {

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void seed(){
        try (Connection conn = dataSource.getConnection()) {
            Utility.seedAll(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
}
