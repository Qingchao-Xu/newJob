package org.xu.newjob;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.xu.newjob.util.MailClient;

@SpringBootTest
@ContextConfiguration(classes = NewJobApplication.class)
public class MailTests {

    @Autowired
    private MailClient mailClient;

    @Test
    public void testTextMail() {
        mailClient.sendMail("2356114320@qq.com", "TEST", "welcome.");
    }
}
