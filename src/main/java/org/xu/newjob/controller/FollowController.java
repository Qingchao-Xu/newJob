package org.xu.newjob.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.xu.newjob.entity.Page;
import org.xu.newjob.entity.User;
import org.xu.newjob.service.FollowService;
import org.xu.newjob.service.UserService;
import org.xu.newjob.util.HostHolder;
import org.xu.newjob.util.NewJobConstant;
import org.xu.newjob.util.NewJobUtil;

import java.util.List;
import java.util.Map;

@Controller
public class FollowController implements NewJobConstant {

    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private FollowService followService;
    @Autowired
    private UserService userService;

    @RequestMapping(value = "/follow", method = RequestMethod.POST)
    @ResponseBody
    public String follow(int entityType, int entityId) {
        User user = hostHolder.getUser();
        followService.follow(user.getId(), entityType, entityId);
        return NewJobUtil.getJSONString(0, "关注成功！");
    }

    @RequestMapping(value = "/unfollow", method = RequestMethod.POST)
    @ResponseBody
    public String unfollow(int entityType, int entityId) {
        User user = hostHolder.getUser();
        followService.unFollow(user.getId(), entityType, entityId);
        return NewJobUtil.getJSONString(0, "取消关注成功");
    }

    @RequestMapping(value = "/followee/{userId}", method = RequestMethod.GET)
    public String getFollowee(@PathVariable("userId") int userId, Page page, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        model.addAttribute("user", user);
        page.setLimit(5);
        page.setPath("/followee/" + userId);
        page.setRows((int) followService.findFolloweeCount(userId, ENTITY_TYPE_USER));

        List<Map<String, Object>> followee = followService.findFollowee(userId, ENTITY_TYPE_USER, page.getOffset(), page.getLimit());

        if (followee != null) {
            for (Map<String, Object> map : followee) {
                User u = (User) map.get("user");
                map.put("hasFollowed", hasFollowed(u.getId()));
            }
        }
        model.addAttribute("users", followee);
        return "/site/followee";
    }

    @RequestMapping(value = "/follower/{userId}", method = RequestMethod.GET)
    public String getFollower(@PathVariable("userId") int userId, Page page, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        model.addAttribute("user", user);
        page.setLimit(5);
        page.setPath("/follower/" + userId);
        page.setRows((int) followService.findFollowerCount(ENTITY_TYPE_USER, userId));

        List<Map<String, Object>> follower = followService.findFollower(userId, ENTITY_TYPE_USER, page.getOffset(), page.getLimit());

        if (follower != null) {
            for (Map<String, Object> map : follower) {
                User u = (User) map.get("user");
                map.put("hasFollowed", hasFollowed(u.getId()));
            }
        }
        model.addAttribute("users", follower);
        return "/site/follower";
    }

    private boolean hasFollowed(int userId) {
        if (hostHolder.getUser() == null) {
            return false;
        }
        return followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
    }
}
