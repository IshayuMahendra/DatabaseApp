package uga.menik.csx370.controllers;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import uga.menik.csx370.models.Post;
import uga.menik.csx370.services.TrendingService;

@Controller
@RequestMapping("/trending")
public class TrendingController {

    private final TrendingService trendingService;

    @Autowired
    public TrendingController(TrendingService trendingService) {
        this.trendingService = trendingService;
    }

    @GetMapping
    public ModelAndView showTrendingPage() {
        ModelAndView mv = new ModelAndView("Trending_page");
        // List<String> hashtags = trendingService.getTrendingHashtags();
        List<Post> trendingPosts = trendingService.getTrendingPosts();
        // mv.addObject("hashtags", hashtags);
        // mv.addObject("posts", posts);
        mv.addObject("posts", trendingPosts);
        return mv;
    }
}
