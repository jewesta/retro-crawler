package com.retrocrawler.core.archive.source;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Archive source backed by a local ZIP file.
 * <p>
 * The configured root path identifies the ZIP file and is also the logical root
 * address exposed by the session. Entry paths are descendants of that address;
 * no ZIP content is extracted to the filesystem.
 */
public final class ZipArchiveSource implements ArchiveSource {

	@Override
	public ArchiveSession open(final Path root) throws IOException {
		return new ZipArchiveSession(root);
	}

	private static final class ZipArchiveSession implements ArchiveSession {

		private final ZipFile archive;

		private final ZipFolder root;

		private final Map<Path, ZipFolder> folders = new LinkedHashMap<>();

		private final Map<Path, ZipArchiveFile> files = new LinkedHashMap<>();

		private final Map<Path, ArchiveListing> listings;

		private boolean closed;

		private ZipArchiveSession(final Path root) throws IOException {
			final Path effectiveRoot = Objects.requireNonNull(root, "root");
			if (!Files.isRegularFile(effectiveRoot)) {
				throw new IllegalArgumentException("Expected a ZIP file but got: " + effectiveRoot);
			}

			archive = new ZipFile(effectiveRoot.toFile());
			this.root = new ZipFolder(this, effectiveRoot);
			folders.put(effectiveRoot, this.root);
			try {
				indexEntries();
				listings = buildListings();
			} catch (final IOException | RuntimeException failure) {
				try {
					archive.close();
				} catch (final IOException closeFailure) {
					failure.addSuppressed(closeFailure);
				}
				throw failure;
			}
		}

		@Override
		public ArchiveFolder root() throws IOException {
			requireOpen();
			return root;
		}

		@Override
		public ArchiveListing list(final ArchiveFolder folder) throws IOException {
			requireOpen();
			final ZipFolder effectiveFolder = requireFolder(folder);
			return listings.get(effectiveFolder.path());
		}

		@Override
		public <T> Optional<T> access(final ArchiveFile file, final ArchiveFileAccessor<T> accessor)
				throws IOException {
			requireOpen();
			final ZipArchiveFile effectiveFile = requireFile(file);
			Objects.requireNonNull(accessor, "accessor");
			try (InputStream content = archive.getInputStream(effectiveFile.entry())) {
				final T result = accessor.access(content);
				return Optional.of(Objects.requireNonNull(result, "accessor result"));
			}
		}

		@Override
		public void close() throws IOException {
			if (!closed) {
				closed = true;
				archive.close();
			}
		}

		private void indexEntries() throws IOException {
			final Set<Path> declaredEntries = new HashSet<>();
			final Enumeration<? extends ZipEntry> entries = archive.entries();
			while (entries.hasMoreElements()) {
				final ZipEntry entry = entries.nextElement();
				final ZipEntryPath entryPath = parse(entry);
				Path current = root.path();
				for (int index = 0; index < entryPath.segments().size(); index++) {
					current = resolve(current, entryPath.segments().get(index), entry);
					final boolean terminal = index == entryPath.segments().size() - 1;
					if (!terminal || entryPath.folder()) {
						registerFolder(current, entry);
					} else {
						registerFile(current, entry);
					}
				}
				if (!declaredEntries.add(current)) {
					throw invalid(entry, "duplicates another entry at the same path");
				}
			}
		}

		private void registerFolder(final Path path, final ZipEntry entry) throws ZipException {
			if (files.containsKey(path)) {
				throw invalid(entry, "collides with a file at " + path);
			}
			folders.computeIfAbsent(path, candidate -> new ZipFolder(this, candidate));
		}

		private void registerFile(final Path path, final ZipEntry entry) throws ZipException {
			if (folders.containsKey(path)) {
				throw invalid(entry, "collides with a folder at " + path);
			}
			if (files.putIfAbsent(path, new ZipArchiveFile(this, path, entry)) != null) {
				throw invalid(entry, "duplicates another file at " + path);
			}
		}

		private Map<Path, ArchiveListing> buildListings() {
			final Map<Path, List<ArchiveFolder>> childFolders = new LinkedHashMap<>();
			final Map<Path, List<ArchiveFile>> childFiles = new LinkedHashMap<>();
			folders.keySet().forEach(path -> {
				childFolders.put(path, new ArrayList<>());
				childFiles.put(path, new ArrayList<>());
			});

			folders.values().stream().filter(folder -> folder != root)
					.forEach(folder -> childFolders.get(folder.path().getParent()).add(folder));
			files.values().forEach(file -> childFiles.get(file.path().getParent()).add(file));

			final Map<Path, ArchiveListing> result = new LinkedHashMap<>();
			folders.keySet().forEach(
					path -> result.put(path, new ArchiveListing(childFolders.get(path), childFiles.get(path))));
			return Collections.unmodifiableMap(result);
		}

		private void requireOpen() throws IOException {
			if (closed) {
				throw new IOException("Archive session is closed.");
			}
		}

		private ZipFolder requireFolder(final ArchiveFolder folder) {
			if (!(folder instanceof ZipFolder candidate) || candidate.session() != this) {
				throw new IllegalArgumentException("Archive folder belongs to another source session.");
			}
			return candidate;
		}

		private ZipArchiveFile requireFile(final ArchiveFile file) {
			if (!(file instanceof ZipArchiveFile candidate) || candidate.session() != this) {
				throw new IllegalArgumentException("Archive file belongs to another source session.");
			}
			return candidate;
		}
	}

	private record ZipFolder(ZipArchiveSession session, Path path) implements ArchiveFolder {

		private ZipFolder {
			Objects.requireNonNull(session, "session");
			Objects.requireNonNull(path, "path");
		}
	}

	private record ZipArchiveFile(ZipArchiveSession session, Path path, ZipEntry entry) implements ArchiveFile {

		private ZipArchiveFile {
			Objects.requireNonNull(session, "session");
			Objects.requireNonNull(path, "path");
			Objects.requireNonNull(entry, "entry");
		}
	}

	private record ZipEntryPath(List<String> segments, boolean folder) {
	}

	private static ZipEntryPath parse(final ZipEntry entry) throws ZipException {
		final String name = entry.getName();
		if (name.isEmpty()) {
			throw invalid(entry, "has an empty name");
		}
		if (name.startsWith("/")) {
			throw invalid(entry, "uses an absolute path");
		}
		if (name.indexOf('\\') >= 0) {
			throw invalid(entry, "uses a backslash instead of the ZIP path separator '/'");
		}

		final String entryName = entry.isDirectory() ? name.substring(0, name.length() - 1) : name;
		if (entryName.isEmpty()) {
			throw invalid(entry, "does not identify an entry below the archive root");
		}

		final List<String> segments = List.of(entryName.split("/", -1));
		for (final String segment : segments) {
			if (segment.isEmpty()) {
				throw invalid(entry, "contains an empty path segment");
			}
			if (".".equals(segment) || "..".equals(segment)) {
				throw invalid(entry, "contains the reserved path segment '" + segment + "'");
			}
		}
		return new ZipEntryPath(segments, entry.isDirectory());
	}

	private static Path resolve(final Path parent, final String segment, final ZipEntry entry) throws ZipException {
		try {
			final Path child = parent.resolve(segment);
			if (!Objects.equals(parent.normalize(), child.normalize().getParent())) {
				throw invalid(entry, "cannot be represented as a direct hierarchical path");
			}
			return child;
		} catch (final InvalidPathException failure) {
			final ZipException invalid = invalid(entry, "cannot be represented as a path");
			invalid.initCause(failure);
			throw invalid;
		}
	}

	private static ZipException invalid(final ZipEntry entry, final String reason) {
		return new ZipException("Invalid ZIP entry '" + entry.getName() + "': " + reason + ".");
	}
}
