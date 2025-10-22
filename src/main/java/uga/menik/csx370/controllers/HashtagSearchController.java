/**
Copyright (c) 2024 Sami Menik, PhD. All rights reserved.

This is a project developed by Dr. Menik to give the students an opportunity to apply database concepts learned in the class in a real world project. Permission is granted to host a running version of this software and to use images or videos of this work solely for the purpose of demonstrating the work to potential employers. Any form of reproduction, distribution, or transmission of the software's source code, in part or whole, without the prior written consent of the copyright owner, is strictly prohibited.
*/
package uga.menik.csx370.controllers;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import uga.menik.csx370.models.Post;
import uga.menik.csx370.utility.Utility;

/*
 * added imports
 */
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Handles /hashtagsearch URL and possibly others.
 * At this point no other URLs.
 */
@Controller
@RequestMapping("/hashtagsearch")
public class HashtagSearchController {

    /**
     * This function handles the /hashtagsearch URL itself.
     * This URL can process a request parameter with name hashtags.
     * In the browser the URL will look something like below:
     * http://localhost:8081/hashtagsearch?hashtags=%23amazing+%23fireworks
     * Note: the value of the hashtags is URL encoded.
     */
    @GetMapping
    public ModelAndView webpage(@RequestParam(name = "hashtags") String hashtags) {
        System.out.println("User is searching: " + hashtags);

        // See notes on ModelAndView in BookmarksController.java.
        ModelAndView mv = new ModelAndView("posts_page");
        /* 
        // Following line populates sample data.
        // You should replace it with actual data from the database.
        List<Post> posts = Utility.createSamplePostsListWithoutComments();
        mv.addObject("posts", posts);

        // If an error occured, you can set the following property with the
        // error message to show the error message to the user.
        // String errorMessage = "Some error occured!";
        // mv.addObject("errorMessage", errorMessage);

        // Enable the following line if you want to show no content message.
        // Do that if your content list is empty.
        // mv.addObject("isNoContent", true);
        */
        // Get hashtags
        String[] parts = hashtags.trim().split("\\s+");
        List<String> tags = new ArrayList<String>();
        for(int i = 0; i < parts.length; i++) {
            String tag = parts[i].trim();
            if(!tag.isEmpty()) {
                if(!tag.startsWith("#")) {
                    tag = "#" + tag;
                } // if
                tags.add(tag.toLowerCase());
            } // if
        } // for

        // Load posts (SAMPLE DATA, CONNECT TO DATABASE AND UPDATE)
        List<Post> allPosts = Utility.createSamplePostsListWithoutComments();
        List<Post> filtered = new ArrayList<Post>();

        // Filter posts with hashtags
        for(Post p : allPosts) {
            String content = p.getContent();
            if(content == null) { continue; }
            String lower = content.toLowerCase();
            
            boolean hasAll = true;
            for(String tag : tags) {
                if(lower.indexOf(tag) == -1) {
                    hasAll = false;
                    break;
                } // if
            } // for

            boolean hasAHash = lower.indexOf('#') != -1;
            if(hasAll && hasAHash) {
                filtered.add(p);
            } // if
        } // for

        // Sort by recent
        java.util.Collections.sort(filtered, new java.util.Comparator<Post>() {
            @Override
            public int compare(Post a, Post b) {
                java.time.LocalDate da = parseDate(a.getPostDate());
                java.time.LocalDate db = parseDate(b.getPostDate());
                return db.compareTo(da);
            } // compare
        }); // sort

        // Add to mv
        mv.addObject("posts", filtered);
        if(filtered.isEmpty()) {
            mv.addObject("isNoContent", true);
        } // if

        return mv;
    }

    // Helper method to help parse date 
    private java.time.LocalDate parseDate(String dateStr) {
        try {
            return java.time.LocalDate.parse(dateStr);
        } catch (Exception e) {
            try {
                return java.time.LocalDate.parse(dateStr.substring(0, 10));
            } catch (Exception e2) {
                return java.time.LocalDate.MIN;
            } // tryCatch
        } // tryCatch
    } // parseDate
    
}
