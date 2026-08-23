package com.techie345.moneys;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureTest {
    @Test
    void verifiesDirectBusinessModulesAndTheirBoundaries() {
        ApplicationModules modules = ApplicationModules.of(MoneyApplication.class);

        assertThat(modules.getModuleForPackage("com.techie345.moneys.identity")).isPresent();
        assertThat(modules.getModuleForPackage("com.techie345.moneys.financial")).isPresent();
        assertThat(modules.getModuleForPackage("com.techie345.moneys.imports")).isPresent();
        assertThat(modules.getModuleForPackage("com.techie345.moneys.dashboard")).isPresent();
        assertThat(modules.getModuleForPackage("com.techie345.moneys.audit")).isPresent();
        assertThat(modules.getModuleForPackage("com.techie345.moneys.sync")).isPresent();
        modules.verify();
    }
}
