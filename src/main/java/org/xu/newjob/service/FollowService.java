package org.xu.newjob.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;
import org.xu.newjob.entity.User;
import org.xu.newjob.util.RedisKeyUtil;

import java.util.*;

@Service
public class FollowService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private UserService userService;

    public void follow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback<>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
                operations.multi();
                redisTemplate.opsForZSet().add(followeeKey, entityId, System.currentTimeMillis());
                redisTemplate.opsForZSet().add(followerKey, userId, System.currentTimeMillis());
                return operations.exec();
            }
        });
    }

    public void unFollow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback<>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
                operations.multi();
                redisTemplate.opsForZSet().remove(followeeKey, entityId);
                redisTemplate.opsForZSet().remove(followerKey, userId);
                return operations.exec();
            }
        });
    }

    // 查询
    public long findFolloweeCount(int userId, int entityType) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        Long size = redisTemplate.opsForZSet().size(followeeKey);
        return size == null ? 0 : size;
    }

    public long findFollowerCount(int entityType, int entityId) {
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
        Long size = redisTemplate.opsForZSet().size(followerKey);
        return size == null ? 0 : size;
    }

    public boolean hasFollowed(int userId, int entityType, int entityId) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().score(followeeKey, entityId) != null;
    }

    // 查询关注列表
    public List<Map<String, Object>> findFollowee(int userId, int entityType, int offset, int limit) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        Set<Object> followeeIds = redisTemplate.opsForZSet().reverseRange(followeeKey, offset, offset + limit - 1);
        if (followeeIds == null) {
            return null;
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object id : followeeIds) {
            Map<String, Object> map = new HashMap<>();
            User user = userService.findUserById((Integer) id);
            map.put("user", user);
            Double score = redisTemplate.opsForZSet().score(followeeKey, id);
            map.put("followTime", new Date(score.longValue()));
            list.add(map);
        }
        return list;
    }

    public List<Map<String, Object>> findFollower(int userId, int entityType, int offset, int limit) {
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, userId);
        Set<Object> followeeIds = redisTemplate.opsForZSet().reverseRange(followerKey, offset, offset + limit - 1);
        if (followeeIds == null) {
            return null;
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object id : followeeIds) {
            Map<String, Object> map = new HashMap<>();
            User user = userService.findUserById((Integer) id);
            map.put("user", user);
            Double score = redisTemplate.opsForZSet().score(followerKey, id);
            map.put("followTime", new Date(score.longValue()));
            list.add(map);
        }
        return list;
    }
}
