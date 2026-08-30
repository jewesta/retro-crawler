package com.retrocrawler.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.Journal;
import com.retrocrawler.core.archive.InMemoryRepository;
import com.retrocrawler.core.archive.ReindexScope;
import com.retrocrawler.demo.DemoModels;
import com.retrocrawler.demo.catalog.DemoId;
import com.retrocrawler.demo.gear.MyKnownGear;

class DemoArchiveTest {

	@Test
	void givesEachAlternativeSourceItsOwnCrawler() throws Exception {
		final List<DemoArchive> archives = DemoArchive.create(DemoModels.RETRO_PC, new InMemoryRepository());

		assertEquals(2, archives.size());
		assertNotSame(archives.get(0).crawler(), archives.get(1).crawler());
		assertEquals(List.of("retro_pc_demo"),
				archives.get(0).crawler().archives().stream().map(archive -> archive.id().value()).toList());
		assertEquals(List.of("retro_pc_demo_zip"),
				archives.get(1).crawler().archives().stream().map(archive -> archive.id().value()).toList());
	}

	@Test
	void crawlsEachAlternativeSourceWithoutCrossSourceIdentityCollisions() throws Exception {
		final List<DemoArchive> archives = DemoArchive.create(DemoModels.RETRO_PC, new InMemoryRepository());
		final List<Set<DemoId>> idsBySource = new ArrayList<>();

		for (final DemoArchive archive : archives) {
			final Set<DemoId> ids = archive.crawler().crawl(new Journal(), ReindexScope.all()).query(MyKnownGear.class)
					.pull().gear().stream().map(MyKnownGear::getId).collect(Collectors.toUnmodifiableSet());
			assertFalse(ids.isEmpty());
			idsBySource.add(ids);
		}

		assertEquals(idsBySource.get(0), idsBySource.get(1));
	}
}
