package org.xu.newjob.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.xu.newjob.entity.DiscussPost;
import org.xu.newjob.service.DiscussPostService;
import org.xu.newjob.service.ElasticSearchService;
import org.xu.newjob.service.LikeService;
import org.xu.newjob.util.NewJobConstant;
import org.xu.newjob.util.RedisKeyUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class PostScoreRefreshJob implements Job, NewJobConstant {

    private static final Logger logger = LoggerFactory.getLogger(PostScoreRefreshJob.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private LikeService likeService;
    @Autowired
    private ElasticSearchService elasticSearchService;

    // 初始化一个常量时间
    private static final Date epoch;

    static {
        try {
            epoch = new SimpleDateFormat("yyyy-MM-dd HH:ss:mm").parse("2014-08-01 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException("初始化纪元时间失败！", e);
        }
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String scoreKey = RedisKeyUtil.getPostScoreKey();
        BoundSetOperations<String, Object> operations = redisTemplate.boundSetOps(scoreKey);
        if (operations.size() == 0) {
            logger.info("[任务取消] 没有需要刷新的帖子！");
            return;
        }
        logger.info("[任务开始] 正在刷新帖子的分数：" + operations.size());
        while (operations.size() > 0) {
            this.refresh((Integer) operations.pop());
        }
        logger.info("[任务结束] 帖子分数刷新完毕！");
    }

    private void refresh(int id) {
        DiscussPost post = discussPostService.findDiscussPostById(id);
        if (post == null) {
            logger.error("该帖子不存在：id = " + id);
            return;
        }
        boolean wonderful = post.getStatus() == 1;
        int commentCount = post.getCommentCount();
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, id);
        double weight = (wonderful ? 75 : 0) + commentCount * 10L + likeCount * 2;
        double score = Math.log10(Math.max(weight, 1)) +
                (double) (post.getCreateTime().getTime() - epoch.getTime()) / (1000 * 3600 * 24);
        discussPostService.updateScore(id, score);
        post.setScore(score);
        elasticSearchService.saveDiscussPost(post);

    }
}
