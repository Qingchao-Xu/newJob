package org.xu.newjob.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.xu.newjob.entity.Event;
import org.xu.newjob.entity.User;
import org.xu.newjob.event.EventProducer;
import org.xu.newjob.service.LikeService;
import org.xu.newjob.util.HostHolder;
import org.xu.newjob.util.NewJobConstant;
import org.xu.newjob.util.NewJobUtil;
import org.xu.newjob.util.RedisKeyUtil;

import java.util.HashMap;
import java.util.Map;

@Controller
public class LikeController implements NewJobConstant {

    @Autowired
    private LikeService likeService;

    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private EventProducer eventProducer;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @RequestMapping(value = "/like", method = RequestMethod.POST)
    @ResponseBody
    public String like(int entityType, int entityId, int entityUserId, int postId) {
        User user = hostHolder.getUser();
        likeService.like(user.getId(), entityType, entityId, entityUserId);

        long likeCount = likeService.findEntityLikeCount(entityType, entityId);
        int likeStatus = likeService.findEntityLikeStatus(user.getId(), entityType, entityId);
        Map<String, Object> map = new HashMap<>();
        map.put("likeCount", likeCount);
        map.put("likeStatus", likeStatus);

        if (likeStatus == 1) {
            Event event = new Event()
                    .setTopic(TOPIC_LIKE)
                    .setUserId(user.getId())
                    .setEntityType(entityType)
                    .setEntityId(entityId)
                    .setEntityUserId(entityUserId)
                    .setData("postId", postId);
            eventProducer.fireEvent(event);
        }

        if (entityId == ENTITY_TYPE_POST) {
            // 添加到缓存中，用于定时任务取出计算帖子分数
            String scoreKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(scoreKey, postId);
        }

        return NewJobUtil.getJSONString(0, null, map);
    }

}
