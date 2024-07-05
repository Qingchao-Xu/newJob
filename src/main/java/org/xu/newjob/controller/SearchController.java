package org.xu.newjob.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.xu.newjob.entity.DiscussPost;
import org.xu.newjob.entity.Page;
import org.xu.newjob.service.ElasticSearchService;
import org.xu.newjob.service.LikeService;
import org.xu.newjob.service.UserService;
import org.xu.newjob.util.NewJobConstant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SearchController implements NewJobConstant {

    @Autowired
    private ElasticSearchService elasticSearchService;
    @Autowired
    private UserService userService;
    @Autowired
    private LikeService likeService;

    // search?keyword=xxx
    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public String search(String keywords, Page page, Model model) {

        org.springframework.data.domain.Page<DiscussPost> searched =
                elasticSearchService.searchDiscussPost(keywords, page.getCurrent() - 1, page.getLimit());

        // 聚合数据
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if (searched != null) {
            for (DiscussPost post : searched) {
                Map<String, Object> map = new HashMap<>();
                map.put("post", post);
                map.put("user", userService.findUserById(post.getUserId()));
                map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));
                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("keywords", keywords);
        page.setPath("/search?keywords=" + keywords);
        page.setRows(searched == null ? 0 : (int) searched.getTotalElements());
        return "/site/search";
    }

}
