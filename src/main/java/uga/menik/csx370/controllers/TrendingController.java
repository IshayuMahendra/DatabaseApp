package uga.menik.csx370.controllers;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import uga.menik.csx370.services.TrendingService;

@Controller
public class TrendingController {

    private final TrendingService trendingService;

    @Autowired
    public TrendingController(TrendingService trendingService) {
        this.trendingService = trendingService;
    }

    @GetMapping("/trending")
    public ModelAndView showTrendingPage() {
        ModelAndView mv = new ModelAndView("trending_page");
        List<String> hashtags = trendingService.getTrendingHashtags();
        mv.addObject("hashtags", hashtags);
        return mv;
    }
}
