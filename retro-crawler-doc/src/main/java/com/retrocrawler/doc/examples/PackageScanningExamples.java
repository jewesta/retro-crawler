package com.retrocrawler.doc.examples;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.retrocrawler.core.RetroCrawler;
import com.retrocrawler.core.RetroCrawlerFactory;
import com.retrocrawler.core.annotation.RetroArchive;
import com.retrocrawler.core.annotation.RetroGear;
import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.core.gear.GearContext;
import com.retrocrawler.core.gear.matcher.GearMatcher;
import com.retrocrawler.doc.facade.classgraph.ClassGraph;
import com.retrocrawler.doc.facade.classgraph.ClassInfo;
import com.retrocrawler.doc.facade.classgraph.ScanResult;
import com.retrocrawler.doc.facade.reflections.Reflections;
import com.retrocrawler.doc.facade.spring.AnnotationTypeFilter;
import com.retrocrawler.doc.facade.spring.BeanDefinition;
import com.retrocrawler.doc.facade.spring.ClassPathScanningCandidateComponentProvider;

/**
 * Implementing package scanning is non-trivial but using one of the many
 * available libraries is. For that reason we have decided against implementing
 * package scanning or forcing bloated Maven dependencies on users. These
 * examples show how to do package scanning to obtain a Set of retro gear types.
 */
public class PackageScanningExamples {

	// Can also be put on any @RetroGear
	@RetroArchive(id = "my_archive", locations = "/my/archive/path")
	public static class MyArchive {
		// doc sample marker type
	}

	public static class MyRetroGearMatcher implements GearMatcher {
		// doc sample marker type

		@Override
		public Confidence matches(final GearContext context) {
			// TODO Auto-generated method stub
			return Confidence.WEAK;
		}
	}

	@RetroGear(MyRetroGearMatcher.class)
	public static class MyRetroGear {
		// doc sample marker type
	}

	public static class MyOtherGearMatcher implements GearMatcher {
		// doc sample marker type

		@Override
		public Confidence matches(final GearContext context) {
			// TODO Auto-generated method stub
			return Confidence.WEAK;
		}
	}

	@RetroGear(MyOtherGearMatcher.class)
	public static class MyOtherGear {
		// doc sample marker type
	}

	public RetroCrawler explicitTypeList() {
		final Set<Class<?>> types = Set.of(MyArchive.class, MyRetroGear.class, MyOtherGear.class);

		return new RetroCrawlerFactory().reflectOn(types);
	}

	public RetroCrawler scanWithClassGraph(final String basePackage) {
		final Set<Class<?>> types;

		try (ScanResult scan = new ClassGraph().acceptPackages(basePackage).enableAnnotationInfo().scan()) {

			types = scan.getAllClasses().stream().filter(
					ci -> ci.hasAnnotation(RetroArchive.class.getName()) || ci.hasAnnotation(RetroGear.class.getName()))
					.map(ClassInfo::loadClass).collect(Collectors.toSet());
		}

		return new RetroCrawlerFactory().reflectOn(types);
	}

	public RetroCrawler scanWithReflections(final String basePackage) {
		final Reflections reflections = new Reflections(basePackage);

		final Set<Class<?>> types = new HashSet<>();
		types.addAll(reflections.getTypesAnnotatedWith(RetroArchive.class));
		types.addAll(reflections.getTypesAnnotatedWith(RetroGear.class));

		return new RetroCrawlerFactory().reflectOn(types);
	}

	public RetroCrawler scanWithSpring(final String basePackage) throws ClassNotFoundException {
		final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(
				false);

		scanner.addIncludeFilter(new AnnotationTypeFilter(RetroArchive.class));
		scanner.addIncludeFilter(new AnnotationTypeFilter(RetroGear.class));

		final Set<Class<?>> types = scanner.findCandidateComponents(basePackage).stream()
				.map(BeanDefinition::getBeanClassName).map(this::loadClass).collect(Collectors.toSet());

		return new RetroCrawlerFactory().reflectOn(types);
	}

	private Class<?> loadClass(final String className) {
		if (className == null) {
			throw new IllegalArgumentException("className");
		}
		try {
			return Class.forName(className);
		} catch (final ClassNotFoundException e) {
			throw new IllegalStateException("Could not load class: " + className, e);
		}
	}

}