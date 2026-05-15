package com.resumeai.template;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Disabled in CI because full application context requires external services")
@SpringBootTest
class TemplateServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
