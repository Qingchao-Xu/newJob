package org.xu.newjob.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.xu.newjob.entity.Comment;
import org.xu.newjob.entity.DiscussPost;
import org.xu.newjob.entity.Event;
import org.xu.newjob.event.EventProducer;
import org.xu.newjob.service.CommentService;
import org.xu.newjob.service.DiscussPostService;
import org.xu.newjob.util.HostHolder;
import org.xu.newjob.util.NewJobConstant;
import org.xu.newjob.util.RedisKeyUtil;

import java.util.Date;

@Controller
@RequestMapping("/comment")
public class CommentController implements NewJobConstant {

    @Autowired
    private CommentService commentService;
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private EventProducer eventProducer;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @RequestMapping(path = "/add/{discussPostId}", method = RequestMethod.POST)
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment) {
        comment.setUserId(hostHolder.getUser().getId());
        comment.setStatus(0);
        comment.setCreateTime(new Date());
        commentService.addComment(comment);

        Event event = new Event()
                .setTopic(TOPIC_COMMENT)
                .setEntityType(comment.getEntityType())
                .setEntityId(comment.getEntityId())
                .setUserId(hostHolder.getUser().getId())
                .setData("postId", discussPostId);
        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            DiscussPost target = discussPostService.findDiscussPostById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        } else if (comment.getEntityType() == ENTITY_TYPE_COMMENT) {
            Comment target = commentService.findCommentById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }
        eventProducer.fireEvent(event);

        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            event = new Event()
              .setTopic(TOPIC_PUBLISH)
              .setUserId(comment.getUserId())
              .setEntityType(ENTITY_TYPE_POST)
              .setEntityId(comment.getEntityId());
            eventProducer.fireEvent(event);

            // 添加到缓存中，用于定时任务取出计算帖子分数
            String scoreKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(scoreKey, discussPostId);
        }

        return "redirect:/discuss/detail/" + discussPostId;
    }
}
