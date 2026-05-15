package com.resumeai.notification;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Disabled in CI because full application context requires external services")
@SpringBootTest
class NotificationServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
