package uga.menik.csx370.controllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import uga.menik.csx370.models.UserStats;
import uga.menik.csx370.services.GameService;
import uga.menik.csx370.services.UserService;

@Controller
@RequestMapping("/profile")
public class ProfileController {
 
    private final UserService userService;
 
    @Autowired
    public ProfileController(UserService userService) {
        this.userService = userService;
    }
 
    @GetMapping
    public ModelAndView webpage(@RequestParam(name = "error", required = false) String error) {
        ModelAndView mv = new ModelAndView("profile_page");
 
        mv.addObject("username", userService.getLoggedInUser().getUsername());
        mv.addObject("errorMessage", error);
 
        return mv;
    }
 
}
