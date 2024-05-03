package org.xu.newjob;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.xu.newjob.util.SensitiveFilter;

@SpringBootTest
@ContextConfiguration(classes = NewJobApplication.class)
public class SensitiveTests {

    @Autowired
    private SensitiveFilter filter;

    @Test
    public void testSensitiveFilter() {
        String text = "这里可以☆赌☆博，嫖☆☆娼，开☆☆票票，哈哈哈！";
        text = filter.filter(text);
        System.out.println(text);
    }
}
