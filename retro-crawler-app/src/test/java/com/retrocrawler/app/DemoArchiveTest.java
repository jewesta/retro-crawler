package com.retrocrawler.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.InMemoryRepository;
import com.retrocrawler.demo.DemoModels;

class DemoArchiveTest {

	@Test
	void sharesOneCrawlerBetweenTheFilesystemAndZipArchives() throws Exception {
		final List<DemoArchive> archives = DemoArchive.create(DemoModels.RETRO_PC, new InMemoryRepository());

		assertEquals(2, archives.size());
		assertSame(archives.get(0).crawler(), archives.get(1).crawler());
		assertEquals(List.of("retro_pc_demo", "retro_pc_demo_zip"),
				archives.getFirst().crawler().archives().stream().map(archive -> archive.id().value()).toList());
	}
}
