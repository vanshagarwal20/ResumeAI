package com.resumeai.eurekaserver;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Disabled in CI because full application context is not needed for SonarCloud analysis")
@SpringBootTest
class EurekaServerApplicationTests {

    @Test
    void contextLoads() {
    }

}
