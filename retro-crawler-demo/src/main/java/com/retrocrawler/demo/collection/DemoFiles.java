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
import com.retrocrawler.core.util.ReadmeWriter;

public final class DemoFiles {

	// Must NOT start with "/" (must be a relative path)
	public static final String DEMO_ARCHIVE_PARENT = "rc_demo_archives";

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
	private static Path copyToWorkDirectory(final Path annotatedLocation) throws IOException {
		Objects.requireNonNull(annotatedLocation, "annotatedLocation");

		if (annotatedLocation.isAbsolute()) {
			throw new IllegalArgumentException(
					"Demo archive materialization requires a relative path but got: " + annotatedLocation);
		}
		if (annotatedLocation.getNameCount() == 0) {
			throw new IllegalArgumentException("Archive path must not be empty: " + annotatedLocation);
		}

		final Path targetRoot = annotatedLocation.normalize();

		// Ensure annotated path is rooted in DEMO_ARCHIVE_PARENT (first path segment).
		if (!DEMO_ARCHIVE_PARENT.equals(targetRoot.getName(0).toString())) {
			throw new IllegalArgumentException(
					"Expected demo archive path to start with " + DEMO_ARCHIVE_PARENT + " but got: " + targetRoot);
		}

		// Ensure DEMO_ARCHIVE_PARENT exists and has the readme (only there).
		final Path demoRoot = Path.of(DEMO_ARCHIVE_PARENT);
		if (Files.exists(demoRoot) && !Files.isDirectory(demoRoot)) {
			throw new IllegalStateException("Expected folder but found a file at: " + demoRoot.toAbsolutePath());
		}
		if (!Files.exists(demoRoot)) {
			Files.createDirectories(demoRoot);
			ReadmeWriter.writeReadme(demoRoot, "This folder is created/managed by RetroCrawler demos.\n");
		}

		// Ensure the parent dirs for the actual target exist (no readme here).
		final Path targetParent = targetRoot.getParent();
		if (targetParent != null) {
			if (Files.exists(targetParent) && !Files.isDirectory(targetParent)) {
				throw new IllegalStateException(
						"Expected folder but found a file at: " + targetParent.toAbsolutePath());
			}
			Files.createDirectories(targetParent);
		}

		if (Files.isDirectory(targetRoot)) {
			/*
			 * The target directory exists: Assume that all the files have already been
			 * copied and just return the directory.
			 */
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