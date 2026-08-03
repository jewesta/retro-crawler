package com.retrocrawler.doc.examples;

import java.nio.file.Path;
import java.util.Set;

import com.retrocrawler.core.Model;
import com.retrocrawler.core.RetroCrawler;
import com.retrocrawler.core.archive.Repository;
import com.retrocrawler.core.gear.TypeSource;
import com.retrocrawler.core.gear.parser.FactParser;

/**
 * The base-package overload is the normal model-discovery path. Explicit types
 * and custom type sources remain available for constrained runtimes and tests.
 */
public class ModelCreationExamples {

	public RetroCrawler discoverBasePackage(final String basePackage, final Repository repository) {
		final Model model = Model.from(basePackage);
		return RetroCrawler.builder().model(model).repository(repository).build();
	}

	public RetroCrawler useExplicitTypes(final Set<Class<?>> types, final Repository repository) {
		final Model model = Model.from(types);
		return RetroCrawler.builder().model(model).repository(repository).build();
	}

	public RetroCrawler useCustomTypeSource(final TypeSource source, final Repository repository) {
		final Model model = Model.from(source);
		return RetroCrawler.builder().model(model).repository(repository).build();
	}

	public RetroCrawler useRuntimeConfiguration(final String basePackage, final Path collectionLocation,
			final Path workingDirectory, final Class<? extends FactParser> parser, final String catalogFile,
			final Repository repository) {
		final Model model = Model.builder().typesFrom(basePackage).locations(collectionLocation)
				.workingDirectory(workingDirectory)
				.factParser(parser, configuration -> configuration.catalogFile(catalogFile)).build();
		return RetroCrawler.builder().model(model).repository(repository).build();
	}
}
