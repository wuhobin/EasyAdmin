package com.nexora.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
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
    static final ArchRule systemMustNotDependOnOtherBusinessDomains = noClasses()
            .that().resideInAnyPackage("com.nexora.system..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.nexora.monitor..",
                    "com.nexora.identity..",
                    "com.nexora.file..",
                    "com.nexora.mail..");

    @ArchTest
    static final ArchRule identityMustOnlyUseSystemAsABusinessDomain = noClasses()
            .that().resideInAnyPackage("com.nexora.identity..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.nexora.monitor..",
                    "com.nexora.file..",
                    "com.nexora.mail..");

    @ArchTest
    static final ArchRule fileMustNotDependOnOtherBusinessDomains = noClasses()
            .that().resideInAnyPackage("com.nexora.file..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.nexora.monitor..",
                    "com.nexora.system..",
                    "com.nexora.identity..",
                    "com.nexora.mail..");

    @ArchTest
    static final ArchRule mailMustOnlyUseSystemAsABusinessDomain = noClasses()
            .that().resideInAnyPackage("com.nexora.mail..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.nexora.monitor..",
                    "com.nexora.identity..",
                    "com.nexora.file..");

    @ArchTest
    static final ArchRule commonMustNotDependOnBusinessDomains = noClasses()
            .that().resideInAnyPackage("com.nexora.cache..", "com.nexora.config..",
                    "com.nexora.constants..", "com.nexora.contract..", "com.nexora.utils..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.nexora.monitor..",
                    "com.nexora.system..",
                    "com.nexora.identity..",
                    "com.nexora.file..",
                    "com.nexora.mail..");

    @ArchTest
    static final ArchRule otherDomainsMustOnlyUseTheSystemApi = noClasses()
            .that().resideOutsideOfPackage("com.nexora.system..")
            .should().dependOnClassesThat(
                    resideInAnyPackage("com.nexora.system..")
                            .and(not(resideInAnyPackage("com.nexora.system.api.."))));
}
