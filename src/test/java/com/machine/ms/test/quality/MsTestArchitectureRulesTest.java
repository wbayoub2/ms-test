package com.machine.ms.test.quality;

import com.machine.common.quality.rules.ArchitectureRules;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class MsTestArchitectureRulesTest {

    @Test
    void appliesCommonArchitectureRules() {
        var importedClasses = new ClassFileImporter().importPackages("com.machine.ms.test");

        ArchitectureRules.controllersShouldNotAccessRepositoriesDirectly().check(importedClasses);
    }
}
