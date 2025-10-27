/**
Copyright (c) 2024 Sami Menik, PhD. All rights reserved.

This is a project developed by Dr. Menik to give the students an opportunity to apply database concepts learned in the class in a real world project. Permission is granted to host a running version of this software and to use images or videos of this work solely for the purpose of demonstrating the work to potential employers. Any form of reproduction, distribution, or transmission of the software's source code, in part or whole, without the prior written consent of the copyright owner, is strictly prohibited.
*/
package uga.menik.csx370.controllers;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import uga.menik.csx370.models.Post;
import uga.menik.csx370.utility.Utility;

/*
 * Added imports
 */
import uga.menik.csx370.services.PostService;
import uga.menik.csx370.services.UserService;
import uga.menik.csx370.models.User;
import java.net.URLEncoder;


/**
 * Handles /bookmarks and its sub URLs.
 * No other URLs at this point.
 * 
 * Learn more about @Controller here: 
 * https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller.html
 */
@Controller
@RequestMapping("/bookmarks")
public class BookmarksController {
    @Autowired
    private PostService postService;
    @Autowired
    private UserService userService;

    /**
     * /bookmarks URL itself is handled by this.
     */
    @GetMapping
    public ModelAndView webpage() {
        // posts_page is a mustache template from src/main/resources/templates.
        // ModelAndView class enables initializing one and populating placeholders
        // in the template using Java objects assigned to named properties.
        ModelAndView mv = new ModelAndView("posts_page");

        // Following line populates sample data.
        // You should replace it with actual data from the database.
        User currentUser = userService.getLoggedInUser();
        if(currentUser == null) {
            return new ModelAndView("redirect:/login");
        } // if

        List<Post> bookmarks = postService.getBookmarkedPosts(currentUser.getUserId());
        mv.addObject("posts", bookmarks);

        // If an error occured, you can set the following property with the
        // error message to show the error message to the user.
        // String errorMessage = "Some error occured!";
        // mv.addObject("errorMessage", errorMessage);

        // Enable the following line if you want to show no content message.
        // Do that if your content list is empty.
        // mv.addObject("isNoContent", true);
        if(bookmarks.isEmpty()) {
            mv.addObject("isNoContent", true);        
        } // if

        return mv;
    }

    /*
     * Adds or removes bookmark
     */
    @GetMapping("/{postId}/bookmark/{isAdd}")
    public String addOrRemoveBookmark(@PathVariable("postId") String postId,
                                      @PathVariable("isAdd") Boolean isAdd) {
        System.out.println("The user is attempting add or remove a bookmark:");
        System.out.println("\tpostId: " + postId);
        System.out.println("\tisAdd: " + isAdd);

        User currentUser = userService.getLoggedInUser();
        if (currentUser == null) {
            return "redirect:/login";
        }

        boolean success;
        int uid = Integer.parseInt(currentUser.getUserId());
        int pid = Integer.parseInt(postId);

        if (isAdd) {
            success = postService.addBookmark(uid, pid);
        } else {
            success = postService.removeBookmark(uid, pid);
        }

        if (success) {
            // Refresh bookmark list after change
            return "redirect:/bookmarks";
        } else {
            String message = URLEncoder.encode(
                "Failed to (un)bookmark the post. Please try again.",
                StandardCharsets.UTF_8
            );
            return "redirect:/bookmarks?error=" + message;
        }
    } //addOrRemoveBookmark
    
}
