package com.retrocrawler.core.archive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.retrocrawler.core.archive.clues.ArchiveNode;
import com.retrocrawler.core.archive.clues.ArchivePathClueFinder;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.FileContentClueFinder;
import com.retrocrawler.core.archive.source.ArchiveFile;
import com.retrocrawler.core.archive.source.ArchiveFileAccessor;
import com.retrocrawler.core.archive.source.ArchiveFolder;
import com.retrocrawler.core.archive.source.ArchiveListing;
import com.retrocrawler.core.archive.source.ArchiveSession;
import com.retrocrawler.core.archive.source.ArchiveSource;
import com.retrocrawler.core.archive.source.ZipArchiveSource;
import com.retrocrawler.core.progress.Progressor;

class ArchiveDiggerSourceTest {

	private static final Path ROOT = Path.of("/provider/archive");

	@TempDir
	private Path temporaryDirectory;

	@Test
	void crawlsAndInspectsAProviderWhosePathsDoNotExistLocally() throws IOException {
		final InMemoryArchiveSource source = InMemoryArchiveSource.withContent("remote evidence");
		final ArchiveDigger digger = digger(source, contentFinder(new AtomicBoolean()));

		final ArchiveNode archive = digger.dig(ROOT, new Progressor());

		final ArchiveNode gear = archive.children().getFirst();
		assertNotNull(gear.artifact());
		assertEquals(Set.of("remote evidence"), clue(gear, "content").value());
		assertTrue(source.sessionClosed);
		assertTrue(source.contentClosed);
	}

	@Test
	void continuesWithoutInvokingContentFinderWhenContentIsUnavailable() throws IOException {
		final AtomicBoolean finderInvoked = new AtomicBoolean();
		final InMemoryArchiveSource source = InMemoryArchiveSource.withoutContent();
		final ArchiveDigger digger = digger(source, contentFinder(finderInvoked));

		final ArchiveNode archive = digger.dig(ROOT, new Progressor());

		final ArchiveNode gear = archive.children().getFirst();
		assertNotNull(gear.artifact());
		assertFalse(finderInvoked.get());
		assertTrue(gear.artifact().clues().stream().noneMatch(clue -> "content".equals(clue.key())));
		assertTrue(source.sessionClosed);
	}

	@Test
	void crawlsAZipArchiveWithoutExtractingIt() throws IOException {
		final Path zip = temporaryDirectory.resolve("collection.zip");
		try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(zip))) {
			output.putNextEntry(new ZipEntry("gear/evidence.txt"));
			output.write("zipped evidence".getBytes(StandardCharsets.UTF_8));
			output.closeEntry();
		}
		final ArchiveDigger digger = digger(zip, new ZipArchiveSource(), contentFinder(new AtomicBoolean()));

		final ArchiveNode archive = digger.dig(zip, new Progressor());

		final ArchiveNode gear = archive.children().getFirst();
		assertNotNull(gear.artifact());
		assertEquals(Set.of("zipped evidence"), clue(gear, "content").value());
		assertFalse(Files.exists(temporaryDirectory.resolve("gear")));
	}

	private static ArchiveDigger digger(final ArchiveSource source, final FileContentClueFinder contentFinder) {
		return digger(ROOT, source, contentFinder);
	}

	private static ArchiveDigger digger(final Path root, final ArchiveSource source,
			final FileContentClueFinder contentFinder) {
		final ArchiveDescriptor descriptor = new ArchiveDescriptor(ArchiveId.of("source_test"), "Source test", root);
		final ArchivePathClueFinder clues = new ArchivePathClueFinder(
				name -> "gear".equals(name) ? Set.of(Clue.of("kind", "gear")) : Set.of(), List.of(contentFinder),
				List.of());
		return new ArchiveDigger(new TestArchiveDefinition(descriptor, clues), source);
	}

	private static FileContentClueFinder contentFinder(final AtomicBoolean invoked) {
		return new FileContentClueFinder() {

			@Override
			public boolean matches(final String fileName) {
				return "evidence.txt".equals(fileName);
			}

			@Override
			public Set<Clue> find(final InputStream content) {
				invoked.set(true);
				try {
					return Set.of(Clue.of("content", new String(content.readAllBytes(), StandardCharsets.UTF_8)));
				} catch (final IOException e) {
					throw new IllegalStateException(e);
				}
			}
		};
	}

	private static Clue clue(final ArchiveNode node, final String key) {
		return node.artifact().clues().stream().filter(candidate -> key.equals(candidate.key())).findFirst()
				.orElseThrow();
	}

	private static final class InMemoryArchiveSource implements ArchiveSource {

		private final byte[] content;

		private boolean sessionClosed;

		private boolean contentClosed;

		private InMemoryArchiveSource(final byte[] content) {
			this.content = content;
		}

		private static InMemoryArchiveSource withContent(final String content) {
			return new InMemoryArchiveSource(content.getBytes(StandardCharsets.UTF_8));
		}

		private static InMemoryArchiveSource withoutContent() {
			return new InMemoryArchiveSource(null);
		}

		@Override
		public ArchiveSession open(final Path root) {
			assertEquals(ROOT, root);
			final MemoryFolder archive = new MemoryFolder(ROOT);
			final MemoryFolder gear = new MemoryFolder(ROOT.resolve("gear"));
			final MemoryFile evidence = new MemoryFile(ROOT.resolve("gear/evidence.txt"));
			final Map<Path, ArchiveListing> listings = Map.of(archive.path(),
					new ArchiveListing(List.of(gear), List.of()), gear.path(),
					new ArchiveListing(List.of(), List.of(evidence)));
			return new ArchiveSession() {

				@Override
				public ArchiveFolder root() {
					return archive;
				}

				@Override
				public ArchiveListing list(final ArchiveFolder folder) {
					return Objects.requireNonNull(listings.get(folder.path()), "folder listing");
				}

				@Override
				public <T> Optional<T> access(final ArchiveFile file, final ArchiveFileAccessor<T> accessor)
						throws IOException {
					if (content == null) {
						return Optional.empty();
					}
					try (InputStream stream = new ByteArrayInputStream(content) {

						@Override
						public void close() throws IOException {
							contentClosed = true;
							super.close();
						}
					}) {
						return Optional.of(Objects.requireNonNull(accessor.access(stream), "accessor result"));
					}
				}

				@Override
				public void close() {
					sessionClosed = true;
				}
			};
		}
	}

	private record MemoryFolder(Path path) implements ArchiveFolder {
	}

	private record MemoryFile(Path path) implements ArchiveFile {
	}
}
