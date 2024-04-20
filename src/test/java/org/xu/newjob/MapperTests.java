package org.xu.newjob;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.xu.newjob.dao.UserMapper;
import org.xu.newjob.entity.User;

@SpringBootTest
@ContextConfiguration(classes = NewJobApplication.class)
public class MapperTests {

    @Autowired
    private UserMapper userMapper;

    @Test
    public void testSelectUser() {
        User user = userMapper.selectById(101);
        System.out.println(user);
        System.out.println(user.getId());
    }

}
