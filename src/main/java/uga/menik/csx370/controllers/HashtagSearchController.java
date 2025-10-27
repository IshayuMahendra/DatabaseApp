/**
Copyright (c) 2024 Sami Menik, PhD. All rights reserved.
*/
package uga.menik.csx370.controllers;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpSession;
import uga.menik.csx370.models.Post;
import uga.menik.csx370.services.PostService;

/**
 * Handles /hashtagsearch URL.
 */
@Controller
@RequestMapping("/hashtagsearch")
public class HashtagSearchController {

    @Autowired
    private PostService postService;

    @GetMapping
    public ModelAndView search(@RequestParam("hashtags") String hashtags, HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return new ModelAndView("redirect:/login");
        }

        String decodedQuery = URLDecoder.decode(hashtags, StandardCharsets.UTF_8);
        List<Post> posts = postService.searchByHashtags(decodedQuery, userId);

        ModelAndView mv = new ModelAndView("posts_page");
        mv.addObject("posts", posts);
        if (posts.isEmpty()) {
            mv.addObject("isNoContent", true);
            mv.addObject("errorMessage", "No posts found for: " + decodedQuery);
        }
        return mv;
    }
}