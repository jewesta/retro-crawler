package com.retrocrawler.core.archive.source;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/** Archive source backed by the NIO filesystem of its configured root path. */
public final class FileSystemArchiveSource implements ArchiveSource {

	@Override
	public ArchiveSession open(final Path root) {
		return new FileSystemArchiveSession(root);
	}

	private static final class FileSystemArchiveSession implements ArchiveSession {

		private final FileSystemFolder root;

		private boolean closed;

		private FileSystemArchiveSession(final Path root) {
			final Path effectiveRoot = Objects.requireNonNull(root, "root");
			if (!Files.isDirectory(effectiveRoot)) {
				throw new IllegalArgumentException("Expected a folder but got: " + effectiveRoot);
			}
			this.root = new FileSystemFolder(this, effectiveRoot);
		}

		@Override
		public ArchiveFolder root() throws IOException {
			requireOpen();
			return root;
		}

		@Override
		public ArchiveListing list(final ArchiveFolder folder) throws IOException {
			requireOpen();
			final FileSystemFolder effectiveFolder = requireFolder(folder);
			final List<ArchiveFolder> folders = new ArrayList<>();
			final List<ArchiveFile> files = new ArrayList<>();
			try (Stream<Path> entries = Files.list(effectiveFolder.path())) {
				entries.forEach(path -> {
					if (Files.isDirectory(path)) {
						folders.add(new FileSystemFolder(this, path));
					} else if (Files.isRegularFile(path)) {
						files.add(new FileSystemFile(this, path));
					}
				});
			}
			return new ArchiveListing(folders, files);
		}

		@Override
		public <T> Optional<T> access(final ArchiveFile file, final ArchiveFileAccessor<T> accessor)
				throws IOException {
			requireOpen();
			final FileSystemFile effectiveFile = requireFile(file);
			Objects.requireNonNull(accessor, "accessor");
			try (InputStream content = Files.newInputStream(effectiveFile.path())) {
				final T result = accessor.access(content);
				return Optional.of(Objects.requireNonNull(result, "accessor result"));
			}
		}

		@Override
		public void close() {
			closed = true;
		}

		private void requireOpen() throws IOException {
			if (closed) {
				throw new IOException("Archive session is closed.");
			}
		}

		private FileSystemFolder requireFolder(final ArchiveFolder folder) {
			if (!(folder instanceof FileSystemFolder candidate) || candidate.session() != this) {
				throw new IllegalArgumentException("Archive folder belongs to another source session.");
			}
			return candidate;
		}

		private FileSystemFile requireFile(final ArchiveFile file) {
			if (!(file instanceof FileSystemFile candidate) || candidate.session() != this) {
				throw new IllegalArgumentException("Archive file belongs to another source session.");
			}
			return candidate;
		}
	}

	private record FileSystemFolder(FileSystemArchiveSession session, Path path) implements ArchiveFolder {

		private FileSystemFolder {
			Objects.requireNonNull(session, "session");
			Objects.requireNonNull(path, "path");
		}
	}

	private record FileSystemFile(FileSystemArchiveSession session, Path path) implements ArchiveFile {

		private FileSystemFile {
			Objects.requireNonNull(session, "session");
			Objects.requireNonNull(path, "path");
		}
	}
}
