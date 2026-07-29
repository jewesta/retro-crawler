package com.retrocrawler;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class LibraryUsePolicyTest {

	@Test
	void shouldNotUseJavaUtilLogging() {
		final JavaClasses classes = new ClassFileImporter().importPackages("com.retrocrawler");
		noClasses().should().dependOnClassesThat().resideInAnyPackage("java.util.logging..").check(classes);
	}

	@Test
	void shouldNotUseSystemOutOrErr() {
		final JavaClasses classes = new ClassFileImporter().importPackages("com.retrocrawler");
		noClasses().should().accessField(System.class, "out").orShould().accessField(System.class, "err")
				.check(classes);
	}

	@Test
	void clueArchiveMustNotDependOnGearResolution() {
		final JavaClasses classes = new ClassFileImporter().importPackages("com.retrocrawler");
		noClasses().that().resideInAnyPackage("com.retrocrawler.core.archive..").should().dependOnClassesThat()
				.resideInAnyPackage("com.retrocrawler.core.gear..").check(classes);
	}

}
