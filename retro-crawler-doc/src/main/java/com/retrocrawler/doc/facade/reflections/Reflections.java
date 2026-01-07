// file: src/main/java/org/reflections/Reflections.java
package com.retrocrawler.doc.facade.reflections;

import java.lang.annotation.Annotation;
import java.util.Set;

public class Reflections {

	public Reflections(final String basePackage) {
		// no-op
	}

	public Set<Class<?>> getTypesAnnotatedWith(final Class<? extends Annotation> annotation) {
		return Set.of();
	}
}