package com.retrocrawler.app;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.retrocrawler.core.archive.source.ArchiveFile;
import com.retrocrawler.core.archive.source.ArchiveFolder;
import com.retrocrawler.core.archive.source.ArchiveListing;
import com.retrocrawler.core.archive.source.ArchiveSession;
import com.retrocrawler.core.archive.source.ArchiveSource;

final class ArchiveSourceFileReader {

	private final ArchiveSource source;

	private final List<Path> roots;

	ArchiveSourceFileReader(final ArchiveSource source, final List<Path> roots) {
		this.source = Objects.requireNonNull(source, "source");
		this.roots = List.copyOf(Objects.requireNonNull(roots, "roots"));
		if (this.roots.isEmpty()) {
			throw new IllegalArgumentException("At least one archive root is required.");
		}
	}

	Optional<byte[]> read(final Path sourcePath) throws IOException {
		final Path requested = Objects.requireNonNull(sourcePath, "sourcePath").normalize();
		final Path configuredRoot = rootFor(requested);
		try (ArchiveSession session = source.open(configuredRoot)) {
			ArchiveFolder current = Objects.requireNonNull(session.root(), "session.root()");
			final Path sourceRoot = current.path().normalize();
			if (!requested.startsWith(sourceRoot)) {
				throw new IllegalArgumentException(
						"Source path '" + sourcePath + "' is not below session root '" + current.path() + "'.");
			}

			final Path relative = sourceRoot.relativize(requested);
			if (relative.toString().isEmpty()) {
				return Optional.empty();
			}
			for (int index = 0; index < relative.getNameCount(); index++) {
				final ArchiveListing listing = Objects.requireNonNull(session.list(current), "session.list(folder)");
				final Path expected = current.path().resolve(relative.getName(index)).normalize();
				final boolean fileName = index == relative.getNameCount() - 1;
				if (fileName) {
					final Optional<ArchiveFile> file = listing.files().stream()
							.filter(candidate -> candidate.path().normalize().equals(expected)).findFirst();
					return file.isEmpty() ? Optional.empty() : session.access(file.get(), InputStream::readAllBytes);
				}
				final Optional<ArchiveFolder> folder = listing.folders().stream()
						.filter(candidate -> candidate.path().normalize().equals(expected)).findFirst();
				if (folder.isEmpty()) {
					return Optional.empty();
				}
				current = folder.get();
			}
			return Optional.empty();
		}
	}

	private Path rootFor(final Path sourcePath) {
		return roots.stream().map(Path::normalize).filter(sourcePath::startsWith)
				.max(Comparator.comparingInt(Path::getNameCount)).orElseThrow(() -> new IllegalArgumentException(
						"Source path is outside the configured archives: " + sourcePath));
	}
}
