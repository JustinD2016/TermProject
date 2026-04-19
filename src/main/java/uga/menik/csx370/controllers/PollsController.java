package uga.menik.csx370.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Connection;
import javax.sql.DataSource;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import uga.menik.csx370.models.Poll;
import uga.menik.csx370.utility.Utility;
import uga.menik.csx370.services.UserService;

/**
 * Handles /post URL and its sub urls.
 */
@Controller
@RequestMapping("/polls")
public class PollsController {

    private final UserService userService;
    private final DataSource dataSource;

    public PollsController(UserService userService, DataSource dataSource) {
        this.userService = userService;
        this.dataSource = dataSource;
    }

    /**
     * This function handles the /polls URL.
     */
    @GetMapping
    public ModelAndView webpage(@RequestParam(name = "error", required = false) String error) {
        ModelAndView mv = new ModelAndView("polls_page");

        /**
         * String sql stores the sql command to return the poll id, poll question, poll creation date, the
         * user who created the poll, their name, the options for their poll, the vote count for each poll option, and whether
         * or not they have voted in the poll and which option they did.
         */

        final String sql = "SELECT " +
            "p.pollId, " +
            "p.question, " +
            "DATE_FORMAT(p.pollDate, '%b %d, %Y, %h:%i %p') AS pollDate, " +
            "u.userId, " +
            "u.firstName, " +
            "u.lastName, " +
            "pop.optionId, " +
            "pop.optionText, " +
            "(SELECT COUNT(*) FROM poll_vote pvo WHERE pvo.optionId = pop.optionId) AS voteCount, " +
            "(SELECT COUNT(*) FROM poll_vote pvo WHERE pvo.optionId = pop.optionId AND pvo.userId = ?) AS userVote, " +
            "(SELECT COUNT(*) FROM poll_vote pvo WHERE pvo.pollId = pop.pollId AND pvo.userId = ?) AS hasVoted " +
            "FROM poll p " +
            "JOIN user u ON p.userId = u.userId " +
            "JOIN poll_option pop ON p.pollId = pop.pollId " +
            "ORDER BY p.pollDate DESC, pop.optionId";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userService.getLoggedInUser().getUserId());
            pstmt.setString(2, userService.getLoggedInUser().getUserId());

            try (ResultSet rs = pstmt.executeQuery()) {
                List<Poll> polls = Utility.convertResultSetToPollList(rs);
                if (polls.isEmpty()) {
                    mv.addObject("isNoContent", true);
                }
                mv.addObject("polls", polls);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            String message = URLEncoder.encode("Failed to load polls. Please try again.", StandardCharsets.UTF_8);
            return new ModelAndView("redirect:/polls?error=" + message);
        }

        mv.addObject("errorMessage", error);
        return mv;
    }
    
    /*
    * Handles creating polls
    * option3 and option4 are not required (A poll must have 2 options though) 
    * */
    @PostMapping("/create")
    public String createPoll(
            @RequestParam(name = "question") String question,
            @RequestParam(name = "option1") String option1,
            @RequestParam(name = "option2") String option2,
            @RequestParam(name = "option3", required = false, defaultValue = "") String option3,
            @RequestParam(name = "option4", required = false, defaultValue = "") String option4) {

        System.out.println("The user is attempting to create a poll:");
        System.out.println("\tquestion: " + question);
        System.out.println("\toption1: " + option1);
        System.out.println("\toption2: " + option2);
        System.out.println("\toption3: " + option3);
        System.out.println("\toption4: " + option4);

        if (question == null || question.trim().isEmpty() || 
            option1 == null || option1.trim().isEmpty() || 
            option2 == null || option2.trim().isEmpty()) {
                String message = URLEncoder.encode("Failed to create poll. Please try again.",
                StandardCharsets.UTF_8);
            return "redirect:/polls?error=" + message;
        }

        /**
         * insPollSQL is a sql command to insert a new poll into the poll table, it stores the userId of the poll
         * creator, the question they wrote, and the current time as the creation time of the poll
         */
        final String insPollSQL = "INSERT INTO poll (userId, question, pollDate) values (?, ?, NOW())";
        /**
         * insOptionSQL is a sql command to insert options into the poll_option table, its used for taking
         * each option in a poll, which each has their own id, and pairs it with the poll id.
         */
        final String insOptionSQL = "INSERT INTO poll_option (pollId, optionText) values (?, ?)";
        /**
         * getId is a sql command to get the poll Id of the latest poll
         */
        final String getId = "SELECT LAST_INSERT_ID()";

        try (Connection conn = dataSource.getConnection();
                    PreparedStatement pollStmt = conn.prepareStatement(insPollSQL)) {

            
            pollStmt.setString(1, userService.getLoggedInUser().getUserId());
            pollStmt.setString(2, question);

            int rowsAffected = pollStmt.executeUpdate();
            
            if (rowsAffected > 0) {
                int pollId;
                try (PreparedStatement pstmt = conn.prepareStatement(getId);
                    ResultSet rs = pstmt.executeQuery()) {
                        if (!rs.next()) {
                            String message = URLEncoder.encode("Failed to create poll. Please try again.",
                                StandardCharsets.UTF_8);
                            return "redirect:/polls/?error=" + message;
                        }
                        pollId = rs.getInt(1);
                    }
                    try (PreparedStatement pstmt = conn.prepareStatement(insOptionSQL)) {
                        pstmt.setInt(1, pollId);
                        pstmt.setString(2, option1);
                        pstmt.executeUpdate();

                        pstmt.setString(2, option2);
                        pstmt.executeUpdate();

                        if (option3 != null && !option3.trim().isEmpty()) {
                            pstmt.setString(2, option3);
                            pstmt.executeUpdate();
                        }
                        if (option4 != null && !option4.trim().isEmpty()) {
                            pstmt.setString(2, option4);
                            pstmt.executeUpdate();
                        }
                    }
                    return "redirect:/polls"; 
            }
                

        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());
        }

        String message = URLEncoder.encode("Failed to create poll. Please try again.",
                StandardCharsets.UTF_8);
        return "redirect:/polls?error=" + message;

    }


 @PostMapping("/{pollId}/vote")
    public String vote(@PathVariable("pollId") String pollId,
        @RequestParam(name = "optionId") String optionId) {

        System.out.println("The user is voting in poll: " + pollId);
        System.out.println("The user is voting on option: " + optionId);

        /**
         * insPollSQL is a sql command to adds a vote to a poll option by storing the pollId, the option in the poll
         * being voted on, and who voted for that option.
         */

        final String insPollSQL = "INSERT INTO poll_vote (pollId, optionId, userId) values (?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
            PreparedStatement pollStmt = conn.prepareStatement(insPollSQL)) {
                
            pollStmt.setString(1, pollId);
            pollStmt.setString(2, optionId);
            pollStmt.setString(3, userService.getLoggedInUser().getUserId());
            
            int rowsAffected = pollStmt.executeUpdate();

            if (rowsAffected > 0) {
                return "redirect:/polls";
            }
 

        } catch (SQLException e) {
            System.err.println("SQL Error: " + e.getMessage());

        }

        String message = URLEncoder.encode("Failed to create poll. Please try again.",
                StandardCharsets.UTF_8);
        return "redirect:/polls?error=" + message;
    }

}