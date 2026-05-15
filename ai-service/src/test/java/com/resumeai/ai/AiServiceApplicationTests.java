package com.resumeai.ai;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Disabled in CI because full application context requires external services")
@SpringBootTest
class AiServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
