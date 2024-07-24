package org.xu.newjob;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = NewJobApplication.class)
public class RedisTests {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 使用 HyperLogLog 类型统计独立总数，HyperLogLog 能够统计数据的独立总数，并且使用的内存非常小，但代价是会有误差
    // 统计 UV 的时候，就存访问网站的 ip 就可以
    @Test
    public void testHyperLogLog() {
        String redisKey = "test:hll:01";
        for (int i = 1; i < 100000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey, i);
        }
        for (int i = 1; i < 100000; i++) {
            int r = (int) (Math.random() * 100000 + 1);
            redisTemplate.opsForHyperLogLog().add(redisKey, r);
        }
        System.out.println(redisTemplate.opsForHyperLogLog().size(redisKey));
    }

    // 将3组数据合并，再统计合并后的独立总数
    // 计算 UV 的时候，可以用来统计某个时间段内的 UV
    @Test
    public void testHyperLogLogUnion() {
        String redisKey2 = "test:hll:02";
        for (int i = 1; i < 10000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey2, i);
        }
        String redisKey3 = "test:hll:03";
        for (int i = 5001; i < 15000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey3, i);
        }
        String redisKey4 = "test:hll:04";
        for (int i = 10000; i < 20000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey4, i);
        }

        String unionKey = "test:hll:union";
        redisTemplate.opsForHyperLogLog().union(unionKey, redisKey2, redisKey3, redisKey4);
        System.out.println(redisTemplate.opsForHyperLogLog().size(unionKey));
    }

    // 使用 BitMap 统计一组数据的布尔值，BitMap可以看成一个byte数组，适合存储大量的连续的布尔值
    // 统计 DAU 的时候，只需要根据用户id，将对应位置设置为 true即可。
    @Test
    public void testBitMap() {
        String redisKey1 = "test:bm:01";
        redisTemplate.opsForValue().setBit(redisKey1, 1, true);
        redisTemplate.opsForValue().setBit(redisKey1, 4, true);
        redisTemplate.opsForValue().setBit(redisKey1, 7, true);
        // 查询某个位置的值
        System.out.println(redisTemplate.opsForValue().getBit(redisKey1, 0));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey1, 1));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey1, 4));
        // 统计所有为 true 的个数
        // 需要通过 redis 底层连接来操作
        Object obj = redisTemplate.execute(
                (RedisCallback<Object>) connection -> connection.stringCommands().bitCount(redisKey1.getBytes()));
        System.out.println(obj);
    }

    // 统计3组数据的布尔值，并对这三组数据做 or 运算
    // 可以用来统计某个时间段内的 AU
    @Test
    public void testBitMapOperation() {
        String redisKey2 = "test:bm:02";
        redisTemplate.opsForValue().setBit(redisKey2, 0, true);
        redisTemplate.opsForValue().setBit(redisKey2, 1, true);
        redisTemplate.opsForValue().setBit(redisKey2, 2, true);
        String redisKey3 = "test:bm:03";
        redisTemplate.opsForValue().setBit(redisKey3, 2, true);
        redisTemplate.opsForValue().setBit(redisKey3, 3, true);
        redisTemplate.opsForValue().setBit(redisKey3, 4, true);
        String redisKey4 = "test:bm:04";
        redisTemplate.opsForValue().setBit(redisKey4, 4, true);
        redisTemplate.opsForValue().setBit(redisKey4, 5, true);
        redisTemplate.opsForValue().setBit(redisKey4, 6, true);

        String redisKey = "test:bm:or";

        Object obj = redisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.stringCommands().bitOp(RedisStringCommands.BitOperation.OR,
                    redisKey.getBytes(), redisKey2.getBytes(), redisKey3.getBytes(), redisKey4.getBytes());
            return connection.stringCommands().bitCount(redisKey.getBytes());
        });
        System.out.println(obj);

    }


}
