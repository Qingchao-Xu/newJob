package org.xu.newjob.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.xu.newjob.util.RedisKeyUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class DataService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");

    public void recordUV(String ip) {
        String uvKey = RedisKeyUtil.getUVKey(df.format(new Date()));
        redisTemplate.opsForHyperLogLog().add(uvKey, ip);
    }

    public long calculateUV(Date start, Date end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("参数不能为空！");
        }
        if (start.after(end)) {
            Date temp = start;
            start = end;
            end = temp;
        }
        // 整理日期范围内的 key
        List<String> keys = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while (!calendar.getTime().after(end)) {
            String key = RedisKeyUtil.getUVKey(df.format(calendar.getTime()));
            keys.add(key);
            calendar.add(Calendar.DATE, 1);
        }
        String uvKey = RedisKeyUtil.getUVKey(df.format(start), df.format(end));
        redisTemplate.opsForHyperLogLog().union(uvKey, keys.toArray(new String[0]));
        return redisTemplate.opsForHyperLogLog().size(uvKey);
    }

    public void recordDAU(int id) {
        String dauKey = RedisKeyUtil.getDAUKey(df.format(new Date()));
        redisTemplate.opsForValue().setBit(dauKey, id, true);
    }

    public long calculateDAU(Date start, Date end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("参数不能为空！");
        }
        if (start.after(end)) {
            Date temp = start;
            start = end;
            end = temp;
        }
        // 整理日期范围内的 key
        List<byte[]> keys = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while (!calendar.getTime().after(end)) {
            String key = RedisKeyUtil.getDAUKey(df.format(calendar.getTime()));
            keys.add(key.getBytes());
            calendar.add(Calendar.DATE, 1);
        }
        String dauKey = RedisKeyUtil.getDAUKey(df.format(start), df.format(end));
        Long dau = redisTemplate.execute((RedisCallback<Long>) connection -> {
            connection.stringCommands().bitOp(RedisStringCommands.BitOperation.OR, dauKey.getBytes(), keys.toArray(new byte[0][0]));
            return connection.stringCommands().bitCount(dauKey.getBytes());
        });
        return dau == null ? 0 : dau;
    }

}
