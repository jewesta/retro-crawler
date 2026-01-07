// file: src/main/java/org/springframework/context/annotation/ClassPathScanningCandidateComponentProvider.java
package com.retrocrawler.doc.facade.spring;

import java.util.Set;

public class ClassPathScanningCandidateComponentProvider {

	public ClassPathScanningCandidateComponentProvider(final boolean useDefaultFilters) {
		// no-op
	}

	public void addIncludeFilter(final AnnotationTypeFilter filter) {
		// no-op
	}

	public Set<BeanDefinition> findCandidateComponents(final String basePackage) {
		return Set.of();
	}
}