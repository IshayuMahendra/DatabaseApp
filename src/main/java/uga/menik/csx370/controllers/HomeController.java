/**
Copyright (c) 2024 Sami Menik, PhD. All rights reserved.

This is a project developed by Dr. Menik to give the students an opportunity to apply database concepts learned in the class in a real world project. Permission is granted to host a running version of this software and to use images or videos of this work solely for the purpose of demonstrating the work to potential employers. Any form of reproduction, distribution, or transmission of the software's source code, in part or whole, without the prior written consent of the copyright owner, is strictly prohibited.
*/
package uga.menik.csx370.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpSession;
import uga.menik.csx370.models.Post;
import uga.menik.csx370.services.PostService;
import uga.menik.csx370.utility.Utility;

/**
 * This controller handles the home page and some of it's sub URLs.
 */
@Controller
@RequestMapping("/")
public class HomeController {

    @Autowired
    private PostService postService;

    /**
     * This is the specific function that handles the root URL itself.
     * 
     * Note that this accepts a URL parameter called error.
     * The value to this parameter can be shown to the user as an error message.
     * See notes in HashtagSearchController.java regarding URL parameters.
     */
    @GetMapping
    public ModelAndView webpage(@RequestParam(name = "error", required = false) String error, HttpSession session) {
        // See notes on ModelAndView in BookmarksController.java.
        ModelAndView mv = new ModelAndView("home_page");

        Integer userId = (Integer) session.getAttribute("userId");

        // If user is logged in → show real DB feed
        if (userId != null) {
            List<Post> posts = postService.getHomeFeed(userId);
            mv.addObject("posts", posts);
        } else {
            // If not logged in → show sample posts (so UI is visible)
            List<Post> posts = Utility.createSamplePostsListWithoutComments();
            mv.addObject("posts", posts);
        }

        // If an error occured, you can set the following property with the
        // error message to show the error message to the user.
        // An error message can be optionally specified with a url query parameter too.
        String errorMessage = error;
        mv.addObject("errorMessage", errorMessage);

        // Enable the following line if you want to show no content message.
        // Do that if your content list is empty.
        // mv.addObject("isNoContent", true);

        return mv;
    }

    /**
     * This function handles the /createpost URL.
     * This handles a post request that is going to be a form submission.
     * The form for this can be found in the home page. The form has a
     * input field with name = posttext. Note that the @RequestParam
     * annotation has the same name. This makes it possible to access the value
     * from the input from the form after it is submitted.
     */
    @PostMapping("/createpost")
    public String createPost(@RequestParam(name = "posttext") String postText, HttpSession session) {
        System.out.println("User is creating post: " + postText);

        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        if (postText == null || postText.trim().isEmpty()) {
            String message = URLEncoder.encode("Post cannot be empty.", StandardCharsets.UTF_8);
            return "redirect:/?error=" + message;
        }

        try {
            postService.createPost(userId, postText.trim());
            return "redirect:/";
        } catch (Exception e) {
            String message = URLEncoder.encode("Failed to create the post: " + e.getMessage(),
                    StandardCharsets.UTF_8);
            return "redirect:/?error=" + message;
        }
    }

    /**
     * Handles GET /home - shows real feed from DB when logged in
     */
    @GetMapping("/home")
    public String home(Model model, HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        List<Post> posts = postService.getHomeFeed(userId);
        model.addAttribute("posts", posts);
        return "home";
    }
}