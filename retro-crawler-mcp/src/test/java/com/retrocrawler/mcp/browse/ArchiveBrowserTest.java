package com.retrocrawler.mcp.browse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.retrocrawler.core.RetroCrawler;
import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.stash.ArchiveCrawlTimes;
import com.retrocrawler.core.stash.ArchiveGear;
import com.retrocrawler.core.stash.CrawlObservation;
import com.retrocrawler.core.stash.GearNode;
import com.retrocrawler.core.stash.Stash;

class ArchiveBrowserTest {

	private static final String COLLECTION_ID = "test_collection";

	private static final ArchiveDescriptor ARCHIVE = new ArchiveDescriptor(ArchiveId.of("hardware"), "Hardware",
			Path.of("provider-root"));

	private static final Instant STARTED = Instant.parse("2026-08-30T12:00:00Z");

	private ArchiveBrowser browser;

	@BeforeEach
	void setUp() throws IOException {
		final ARI ibm = ari("Systems", "IBM");
		final GearNode<Object> computer = new GearNode<>(new Computer(), ibm,
				List.of(new GearNode<>(new Component(), ari("Systems", "IBM", "Components", "Memory"), List.of())));
		final GearNode<Object> game = new GearNode<>(new Game(), ari("Software", "Games"), List.of());
		final Stash stash = new Stash(List.of(new ArchiveGear<>(ARCHIVE, List.of(computer, game))), List.of(),
				List.of(new ArchiveCrawlTimes(ARCHIVE, observations())));
		final RetroCrawler crawler = mock(RetroCrawler.class);
		when(crawler.collectionId()).thenReturn(COLLECTION_ID);
		when(crawler.archives()).thenReturn(List.of(ARCHIVE));
		when(crawler.access(any())).thenReturn(stash);
		browser = new ArchiveBrowser(crawler);
	}

	@Test
	void returnsBoundedDirectChildrenWithFolderAndGearMetadata() throws IOException {
		final ArchiveFolderPage firstPage = browser.browse(ari().toString(), null, 1);

		assertThat(firstPage.folder()).isEqualTo(new ArchiveFolderSummary(ari().toString(), "Hardware", 2, 3, List.of(),
				STARTED.toString(), observed(0).toString()));
		assertThat(firstPage.totalChildren()).isEqualTo(2);
		assertThat(firstPage.offset()).isZero();
		assertThat(firstPage.limit()).isEqualTo(1);
		assertThat(firstPage.hasMore()).isTrue();
		assertThat(firstPage.children()).containsExactly(new ArchiveFolderSummary(ari("Systems").toString(), "Systems",
				1, 2, List.of(), STARTED.toString(), observed(1).toString()));

		final ArchiveFolderPage ibm = browser.browse(ari("Systems").toString(), null, null);

		assertThat(ibm.limit()).isEqualTo(ArchiveBrowser.DEFAULT_LIMIT);
		assertThat(ibm.children()).containsExactly(new ArchiveFolderSummary(ari("Systems", "IBM").toString(), "IBM", 1,
				2, List.of("Computer"), STARTED.toString(), observed(2).toString()));
	}

	@Test
	void supportsOffsetsBeyondTheAvailableChildren() throws IOException {
		final ArchiveFolderPage page = browser.browse(ari("Software").toString(), 10, 5);

		assertThat(page.totalChildren()).isEqualTo(1);
		assertThat(page.offset()).isEqualTo(10);
		assertThat(page.hasMore()).isFalse();
		assertThat(page.children()).isEmpty();
	}

	@Test
	void rejectsArisOutsideThePublishedFolderIndex() {
		assertThatIllegalArgumentException().isThrownBy(() -> browser.browse("not-an-ari", null, null))
				.withMessage("Invalid archive folder ARI: not-an-ari");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> browser
						.browse(ARI.of("another_collection", ARCHIVE.id(), Path.of("Systems")).toString(), null, null))
				.withMessageStartingWith("Archive folder ARI belongs to collection 'another_collection'");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> browser.browse(
						ARI.of(COLLECTION_ID, ArchiveId.of("missing"), Path.of("Systems")).toString(), null, null))
				.withMessage("Unknown archive id in folder ARI: missing");
		assertThatIllegalArgumentException().isThrownBy(() -> browser.browse(ari("Missing").toString(), null, null))
				.withMessage("Unknown indexed folder ARI: ari:/test_collection/hardware/Missing");
		assertThatIllegalArgumentException().isThrownBy(() -> browser.browse(ari().toString(), null, 101))
				.withMessage("limit must be between 1 and 100.");
	}

	private static Map<ARI, CrawlObservation> observations() {
		final Map<ARI, CrawlObservation> observations = new LinkedHashMap<>();
		observe(observations, 0);
		observe(observations, 1, "Systems");
		observe(observations, 2, "Systems", "IBM");
		observe(observations, 3, "Systems", "IBM", "Components");
		observe(observations, 4, "Systems", "IBM", "Components", "Memory");
		observe(observations, 5, "Software");
		observe(observations, 6, "Software", "Games");
		return observations;
	}

	private static void observe(final Map<ARI, CrawlObservation> observations, final int seconds,
			final String... folders) {
		observations.put(ari(folders), new CrawlObservation(STARTED, observed(seconds)));
	}

	private static Instant observed(final int seconds) {
		return STARTED.plusSeconds(seconds);
	}

	private static ARI ari(final String... folders) {
		Path path = Path.of("");
		for (final String folder : folders) {
			path = path.resolve(folder);
		}
		return ARI.of(COLLECTION_ID, ARCHIVE.id(), path);
	}

	private record Computer() {
	}

	private record Component() {
	}

	private record Game() {
	}
}
