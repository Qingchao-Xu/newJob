package org.xu.newjob.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.xu.newjob.util.RedisKeyUtil;

@Service
public class LikeService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 点赞
    public void like(int userId, int entityType, int entityId) {
        String key = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        boolean isMember = redisTemplate.opsForSet().isMember(key, userId);
        if (isMember) {
            redisTemplate.opsForSet().remove(key, userId);
        } else {
            redisTemplate.opsForSet().add(key, userId);
        }
    }

    // 查询点赞数量
    public long findEntityLikeCount(int entityType, int entityId) {
        String key = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().size(key);
    }

    // 查询点赞状态
    public int findEntityLikeStatus(int userId, int entityType, int entityId) {
        String key = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().isMember(key, userId) ? 1 : 0;
    }
}
