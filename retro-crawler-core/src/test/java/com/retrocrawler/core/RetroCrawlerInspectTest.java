package com.retrocrawler.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.archive.InMemoryRepository;
import com.retrocrawler.core.archive.source.ArchiveFile;
import com.retrocrawler.core.archive.source.ArchiveFileAccessor;
import com.retrocrawler.core.archive.source.ArchiveFolder;
import com.retrocrawler.core.archive.source.ArchiveListing;
import com.retrocrawler.core.archive.source.ArchiveSession;
import com.retrocrawler.core.archive.source.ArchiveSource;
import com.retrocrawler.core.archive.source.FileSystemArchiveSource;
import com.retrocrawler.core.archive.source.ZipArchiveSource;

class RetroCrawlerInspectTest {

	private static final ArchiveId ARCHIVE_ID = ArchiveId.of("test_archive");

	@TempDir
	private Path temporaryDirectory;

	@Test
	void inspectsAFileFromTheFilesystemSource() throws IOException {
		final Path root = Files.createDirectory(temporaryDirectory.resolve("archive"));
		Files.writeString(Files.createDirectory(root.resolve("gear")).resolve("front.jpeg"), "filesystem image");
		final RetroCrawler crawler = crawler(root, new FileSystemArchiveSource());

		final Optional<byte[]> content = crawler.inspect(ari(Path.of("gear/front.jpeg")), InputStream::readAllBytes);

		assertTrue(content.isPresent());
		assertArrayEquals("filesystem image".getBytes(StandardCharsets.UTF_8), content.get());
	}

	@Test
	void inspectsAFileFromTheZipSourceWithoutExtraction() throws IOException {
		final Path archive = temporaryDirectory.resolve("archive.zip");
		try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
			output.putNextEntry(new ZipEntry("gear/front.jpeg"));
			output.write("zip image".getBytes(StandardCharsets.UTF_8));
			output.closeEntry();
		}
		final RetroCrawler crawler = crawler(archive, new ZipArchiveSource());

		final Optional<byte[]> content = crawler.inspect(ari(Path.of("gear/front.jpeg")), InputStream::readAllBytes);

		assertTrue(content.isPresent());
		assertArrayEquals("zip image".getBytes(StandardCharsets.UTF_8), content.get());
		assertTrue(Files.notExists(temporaryDirectory.resolve("gear")));
	}

	@Test
	void returnsEmptyWhenTheSourceDoesNotExposeKnownFileContent() throws IOException {
		final Path root = Path.of("remote/archive");
		final Path filePath = root.resolve("manual.pdf");
		final AtomicBoolean closed = new AtomicBoolean();
		final ArchiveSource source = ignored -> new ArchiveSession() {

			private final ArchiveFolder folder = () -> root;

			private final ArchiveFile file = () -> filePath;

			@Override
			public ArchiveFolder root() {
				return folder;
			}

			@Override
			public ArchiveListing list(final ArchiveFolder ignoredFolder) {
				return new ArchiveListing(List.of(), List.of(file));
			}

			@Override
			public <T> Optional<T> access(final ArchiveFile ignoredFile,
					final ArchiveFileAccessor<T> ignoredInspector) {
				return Optional.empty();
			}

			@Override
			public void close() {
				closed.set(true);
			}
		};
		final RetroCrawler crawler = crawler(root, source);

		final Optional<Integer> result = crawler.inspect(ari(Path.of("manual.pdf")), input -> {
			throw new AssertionError("Unavailable content must not invoke the inspector.");
		});

		assertTrue(result.isEmpty());
		assertTrue(closed.get());
	}

	@Test
	void rejectsAnAddressThatDoesNotIdentifyAFile() throws IOException {
		final Path root = Files.createDirectory(temporaryDirectory.resolve("archive"));
		final RetroCrawler crawler = crawler(root, new FileSystemArchiveSource());
		final ARI source = ari(Path.of("missing.jpeg"));

		final NoSuchFileException failure = assertThrows(NoSuchFileException.class,
				() -> crawler.inspect(source, InputStream::readAllBytes));

		assertEquals(source.toString(), failure.getFile());
	}

	@Test
	void rejectsAnAddressThatIdentifiesAFolder() throws IOException {
		final Path root = Files.createDirectory(temporaryDirectory.resolve("archive"));
		Files.createDirectory(root.resolve("gear"));
		final RetroCrawler crawler = crawler(root, new FileSystemArchiveSource());
		final ARI source = ari(Path.of("gear"));

		final NoSuchFileException failure = assertThrows(NoSuchFileException.class,
				() -> crawler.inspect(source, InputStream::readAllBytes));

		assertEquals(source.toString(), failure.getFile());
	}

	@Test
	void rejectsAnAriFromAnotherCollectionBeforeOpeningTheSource() {
		final Path root = temporaryDirectory.resolve("archive");
		final RetroCrawler crawler = crawler(root, ignored -> {
			throw new AssertionError("A foreign ARI must not open the source.");
		});
		final ARI foreign = ARI.of("another_collection", ARCHIVE_ID, Path.of("front.jpeg"));

		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> crawler.inspect(foreign, InputStream::readAllBytes));

		assertTrue(failure.getMessage().contains("another_collection"));
		assertTrue(failure.getMessage().contains("factory_test"));
	}

	@Test
	void resolvesAnAriFromAnotherCrawlerForTheSameCollectionAndArchive() throws IOException {
		final Path firstRoot = Files.createDirectory(temporaryDirectory.resolve("first"));
		final Path secondRoot = Files.createDirectory(temporaryDirectory.resolve("second"));
		Files.writeString(firstRoot.resolve("evidence.txt"), "first");
		Files.writeString(secondRoot.resolve("evidence.txt"), "second");
		final RetroCrawler first = crawler(firstRoot, new FileSystemArchiveSource());
		final RetroCrawler second = crawler(secondRoot, new FileSystemArchiveSource());

		final ARI source = ari(Path.of("evidence.txt"));
		assertArrayEquals("first".getBytes(StandardCharsets.UTF_8),
				first.inspect(source, InputStream::readAllBytes).orElseThrow());
		final Optional<byte[]> content = second.inspect(source, InputStream::readAllBytes);

		assertArrayEquals("second".getBytes(StandardCharsets.UTF_8), content.orElseThrow());
	}

	private static RetroCrawler crawler(final Path root, final ArchiveSource source) {
		final Model model = Model.from(
				Set.of(RetroCrawlerBuilderTest.TestArchiveConfiguration.class, RetroCrawlerBuilderTest.TestGear.class));
		return RetroCrawler.builder().model(model).repository(new InMemoryRepository())
				.archive(ArchiveDescriptor.of(ARCHIVE_ID, root), source).build();
	}

	private static ARI ari(final Path resourcePath) {
		return ARI.of("factory_test", ARCHIVE_ID, resourcePath);
	}
}
