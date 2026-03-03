package com.machine.ms.test;

import com.machine.ms.test.infra.config.MsTestIntegrationProperties;
import com.machine.ms.test.infra.config.MsTestPersistenceProperties;
import com.machine.ms.test.infra.config.MsTestStepValidationProperties;
import com.machine.ms.test.infra.config.MsInfraClientProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        MsTestIntegrationProperties.class,
        MsTestPersistenceProperties.class,
        MsInfraClientProperties.class,
        MsTestStepValidationProperties.class
})
public class MsTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsTestApplication.class, args);
    }
}
