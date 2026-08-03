package com.retrocrawler.core.gear;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;

import com.retrocrawler.core.catalog.Catalog;
import com.retrocrawler.core.catalog.CatalogLoader;
import com.retrocrawler.core.gear.parser.FactParserConfiguration;

final class DefaultCatalogLoader implements CatalogLoader {

	private static final String CATALOG_DIRECTORY = "catalogs";

	private final Class<?> parserType;
	private final Path workingDirectory;
	private final FactParserConfiguration configuration;

	DefaultCatalogLoader(final Class<?> parserType, final Path workingDirectory,
			final FactParserConfiguration configuration) {
		this.parserType = Objects.requireNonNull(parserType, "parserType");
		this.workingDirectory = workingDirectory;
		this.configuration = configuration;
	}

	@Override
	public <K extends Enum<K>> Catalog<K> load(final Class<K> keyType, final String defaultCatalogFile) {
		Objects.requireNonNull(keyType, "keyType");
		Objects.requireNonNull(defaultCatalogFile, "defaultCatalogFile");

		if (configuration != null && configuration.catalogFile().isPresent()) {
			return loadExternal(keyType, configuration.catalogFile().orElseThrow(), true);
		}

		try (InputStream input = parserType.getResourceAsStream(defaultCatalogFile)) {
			if (input != null) {
				return Catalog.read(keyType, new InputStreamReader(input, StandardCharsets.UTF_8));
			}
		} catch (final IOException e) {
			throw new UncheckedIOException("Could not close bundled catalog '" + defaultCatalogFile
					+ "' for parser " + parserType.getName() + ".", e);
		} catch (final IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid bundled catalog '" + defaultCatalogFile
					+ "' for parser " + parserType.getName() + ".", e);
		}

		return loadExternal(keyType, defaultCatalogFile, false);
	}

	private <K extends Enum<K>> Catalog<K> loadExternal(final Class<K> keyType, final String catalogFile,
			final boolean explicitlyConfigured) {
		if (workingDirectory == null) {
			final String source = explicitlyConfigured ? "Configured catalog" : "Default catalog";
			throw new IllegalStateException(source + " '" + catalogFile + "' for parser " + parserType.getName()
					+ " requires a collection working directory.");
		}

		final Path relative = relativeCatalogPath(catalogFile);
		final Path catalogRoot = workingDirectory.toAbsolutePath().normalize().resolve(CATALOG_DIRECTORY);
		final Path source = catalogRoot.resolve(relative).normalize();
		if (!source.startsWith(catalogRoot)) {
			throw new IllegalArgumentException("Catalog file escapes the collection catalog directory: "
					+ catalogFile);
		}
		if (!Files.isRegularFile(source)) {
			throw new IllegalStateException("Catalog file for parser " + parserType.getName()
					+ " does not exist or is not a regular file: " + source);
		}

		try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
			return Catalog.read(keyType, reader);
		} catch (final IOException e) {
			throw new UncheckedIOException("Could not read catalog file for parser " + parserType.getName()
					+ ": " + source, e);
		} catch (final IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid catalog file for parser " + parserType.getName()
					+ ": " + source, e);
		}
	}

	private static Path relativeCatalogPath(final String catalogFile) {
		final String normalizedName = Objects.requireNonNull(catalogFile, "catalogFile").trim();
		if (normalizedName.isEmpty()) {
			throw new IllegalArgumentException("catalogFile must not be blank.");
		}
		try {
			final Path relative = Path.of(normalizedName).normalize();
			if (relative.isAbsolute() || relative.getNameCount() == 0 || relative.startsWith("..")) {
				throw new IllegalArgumentException("Catalog file must be a relative path below the collection "
						+ "catalog directory: " + catalogFile);
			}
			return relative;
		} catch (final InvalidPathException e) {
			throw new IllegalArgumentException("Invalid catalog file path: " + catalogFile, e);
		}
	}
}
