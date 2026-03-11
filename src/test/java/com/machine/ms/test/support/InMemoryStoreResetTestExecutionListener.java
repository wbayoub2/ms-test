package com.machine.ms.test.support;

import com.machine.ms.test.infra.persistence.ResettableInMemoryStore;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

public class InMemoryStoreResetTestExecutionListener extends AbstractTestExecutionListener {

    @Override
    public void beforeTestMethod(TestContext testContext) {
        testContext.getApplicationContext()
                .getBeansOfType(ResettableInMemoryStore.class)
                .values()
                .forEach(ResettableInMemoryStore::reset);
    }
}
