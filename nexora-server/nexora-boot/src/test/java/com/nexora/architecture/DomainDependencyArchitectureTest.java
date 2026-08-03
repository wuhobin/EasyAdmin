package com.nexora.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.nexora", importOptions = ImportOption.DoNotIncludeTests.class)
class DomainDependencyArchitectureTest {

    @ArchTest
    static final ArchRule monitorMustNotDependOnOtherBusinessDomains = noClasses()
            .that().resideInAnyPackage("com.nexora.monitor..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.nexora.system..",
                    "com.nexora.identity..",
                    "com.nexora.file..",
                    "com.nexora.mail..");

    @ArchTest
    static final ArchRule systemMustNotDependOnUpstreamBusinessDomains = noClasses()
            .that().resideInAnyPackage("com.nexora.system..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.nexora.identity..",
                    "com.nexora.file..",
                    "com.nexora.mail..");

    @ArchTest
    static final ArchRule identityMustNotDependOnFileOrMail = noClasses()
            .that().resideInAnyPackage("com.nexora.identity..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.nexora.file..",
                    "com.nexora.mail..");

    @ArchTest
    static final ArchRule fileMustOnlyUseIdentityAsABusinessDomain = noClasses()
            .that().resideInAnyPackage("com.nexora.file..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.nexora.monitor..",
                    "com.nexora.system..",
                    "com.nexora.mail..");

    @ArchTest
    static final ArchRule mailMustNotDependOnFileOrMonitor = noClasses()
            .that().resideInAnyPackage("com.nexora.mail..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.nexora.monitor..",
                    "com.nexora.file..");
}
