 package uga.menik.csx370.controllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import uga.menik.csx370.models.Actor;
import uga.menik.csx370.services.GameService;

@Controller
@RequestMapping("/lookup")
public class LookupController {

    private final DataSource dataSource;
    private final GameService gameService;

    public LookupController(DataSource dataSource, GameService gameService) {
        this.dataSource = dataSource;
        this.gameService = gameService;
    }

    @GetMapping
public ModelAndView webpage(@RequestParam(name = "actorname", required = false) String actorName) {
    ModelAndView mv = new ModelAndView("lookup_page");

    mv.addObject("actorName", "");

    if (actorName == null || actorName.trim().isEmpty()) {
        mv.addObject("searched", false);
        return mv;
    }

    mv.addObject("searched", true);
    mv.addObject("actorName", actorName);

        String sql =
            "SELECT actor_id FROM actor " +
            "WHERE CONCAT(first_name, ' ', last_name) LIKE ? " +
            "ORDER BY last_name, first_name " +
            "LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + actorName.trim() + "%");

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Actor actor = gameService.loadActor(conn, rs.getString("actor_id"));
                    mv.addObject("actor", actor);
                    mv.addObject("foundActor", true);
                } else {
                    mv.addObject("foundActor", false);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            mv.addObject("errorMessage", "Failed to load actor data.");
        }

        return mv;
    }
}