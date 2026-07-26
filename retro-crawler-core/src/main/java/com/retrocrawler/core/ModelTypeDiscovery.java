package com.retrocrawler.core;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.retrocrawler.core.annotation.RetroArchive;
import com.retrocrawler.core.annotation.RetroGear;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

final class ModelTypeDiscovery {

	private ModelTypeDiscovery() {
		// static utility class
	}

	static Set<Class<?>> discover(final String basePackage) {
		Objects.requireNonNull(basePackage, "basePackage");

		final String effectiveBasePackage = basePackage.trim();
		if (effectiveBasePackage.isEmpty()) {
			throw new IllegalArgumentException("basePackage must not be blank.");
		}

		final Set<Class<?>> types;
		try (ScanResult scan = new ClassGraph().acceptPackages(effectiveBasePackage).enableAnnotationInfo()
				.ignoreClassVisibility().scan()) {
			types = Stream
					.concat(scan.getClassesWithAnnotation(RetroArchive.class.getName()).stream(),
							scan.getClassesWithAnnotation(RetroGear.class.getName()).stream())
					.distinct().sorted(Comparator.comparing(ClassInfo::getName))
					.map(classInfo -> loadClass(classInfo, effectiveBasePackage))
					.collect(Collectors.toCollection(LinkedHashSet::new));
		} catch (final IllegalStateException e) {
			throw e;
		} catch (final RuntimeException e) {
			throw new IllegalStateException(
					"Could not discover RetroCrawler model types in base package '" + effectiveBasePackage + "'.", e);
		}

		if (types.isEmpty()) {
			throw new IllegalArgumentException("No types annotated with @" + RetroArchive.class.getSimpleName()
					+ " or @" + RetroGear.class.getSimpleName() + " found in base package '" + effectiveBasePackage
					+ "'.");
		}

		return types;
	}

	private static Class<?> loadClass(final ClassInfo classInfo, final String basePackage) {
		try {
			return classInfo.loadClass();
		} catch (final RuntimeException | LinkageError e) {
			throw new IllegalStateException("Could not load discovered model type '" + classInfo.getName()
					+ "' from base package '" + basePackage + "'.", e);
		}
	}
}
