package com.retrocrawler.demo.collection;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import com.retrocrawler.core.archive.ArchiveDescriptor;

public final class DemoFiles {

	private DemoFiles() {
		// no instances
	}

	/**
	 * Ensures the given archive folder exists on the local filesystem.
	 *
	 * <p>
	 * Demo contract:
	 * <ul>
	 * <li>{@code annotatedLocation} is the location configured in
	 * {@code @RetroArchive}, e.g. {@code Path.of("archives/retro_pc")}</li>
	 * <li>If the folder exists locally: do nothing</li>
	 * <li>Otherwise: attempt to copy it from classpath at
	 * {@code "/" + annotatedLocation}</li>
	 * <li>Fail if the classpath folder does not exist</li>
	 * </ul>
	 *
	 * @return the local path (existing or newly created)
	 */
	public static Path copyToWorkDirectory(final Path annotatedLocation) throws IOException {
		Objects.requireNonNull(annotatedLocation, "annotatedLocation");

		if (annotatedLocation.isAbsolute()) {
			throw new IllegalArgumentException(
					"Demo archive materialization requires a relative path but got: " + annotatedLocation);
		}
		if (annotatedLocation.getNameCount() == 0) {
			throw new IllegalArgumentException("Archive path must not be empty: " + annotatedLocation);
		}

		final Path targetRoot = annotatedLocation.normalize();

		if (Files.isDirectory(targetRoot)) {
			return targetRoot;
		}
		if (Files.exists(targetRoot) && !Files.isDirectory(targetRoot)) {
			throw new IllegalStateException("Expected folder but found a file at: " + targetRoot.toAbsolutePath());
		}

		final String classpathFolder = toClasspathFolder(targetRoot);

		final URL url = DemoFiles.class.getResource(classpathFolder);
		if (url == null) {
			throw new IllegalStateException("Demo archive not found on classpath at: " + classpathFolder
					+ " (expected it in src/main/resources" + classpathFolder + ")");
		}

		final URI uri = toUri(url, classpathFolder);

		final Path parent = targetRoot.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}

		copyFromResourceUri(uri, classpathFolder, targetRoot);

		if (!Files.isDirectory(targetRoot)) {
			throw new IllegalStateException(
					"Materialization failed, folder not created: " + targetRoot.toAbsolutePath());
		}

		return targetRoot;
	}

	public static void copyToWorkDirectory(final ArchiveDescriptor descriptor) throws IOException {
		for (final Path path : descriptor.getPaths()) {
			copyToWorkDirectory(path);
		}
	}

	private static String toClasspathFolder(final Path relativePath) {
		// Turn Path into a classpath folder, always using forward slashes.
		// Path "archives/retro_pc" -> "/archives/retro_pc"
		final String p = relativePath.toString().replace('\\', '/');
		return "/" + p;
	}

	private static URI toUri(final URL url, final String classpathFolder) throws IOException {
		try {
			return url.toURI();
		} catch (final URISyntaxException e) {
			throw new IOException("Failed to read resource URI for: " + classpathFolder + " (" + url + ")", e);
		}
	}

	private static void copyFromResourceUri(final URI uri, final String classpathFolder, final Path targetRoot)
			throws IOException {

		final String scheme = uri.getScheme();

		if ("file".equalsIgnoreCase(scheme)) {
			copyFolder(Paths.get(uri), targetRoot);
			return;
		}

		if ("jar".equalsIgnoreCase(scheme)) {
			final FileSystem fs = openJarFileSystem(uri);
			final Path sourceRoot = fs.getPath(classpathFolder);
			copyFolder(sourceRoot, targetRoot);
			return;
		}

		throw new IOException("Unsupported resource URI scheme: " + scheme + " (" + uri + ")");
	}

	private static FileSystem openJarFileSystem(final URI jarUri) throws IOException {
		try {
			return FileSystems.newFileSystem(jarUri, Map.of());
		} catch (final FileSystemAlreadyExistsException e) {
			return FileSystems.getFileSystem(jarUri);
		}
	}

	private static void copyFolder(final Path sourceRoot, final Path targetRoot) throws IOException {
		Files.createDirectories(targetRoot);

		try (Stream<Path> stream = Files.walk(sourceRoot)) {
			stream.forEach(source -> {
				try {
					final Path rel = sourceRoot.relativize(source);
					final Path dest = targetRoot.resolve(rel.toString());

					if (Files.isDirectory(source)) {
						Files.createDirectories(dest);
						return;
					}

					Files.createDirectories(dest.getParent());
					Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
				} catch (final IOException e) {
					throw new UncheckedIOExceptionWrapper(e);
				}
			});
		} catch (final UncheckedIOExceptionWrapper e) {
			throw e.getIOException();
		}
	}

	private static final class UncheckedIOExceptionWrapper extends RuntimeException {
		private static final long serialVersionUID = 1L;

		private final IOException ioe;

		UncheckedIOExceptionWrapper(final IOException ioe) {
			super(ioe);
			this.ioe = ioe;
		}

		IOException getIOException() {
			return ioe;
		}
	}
}