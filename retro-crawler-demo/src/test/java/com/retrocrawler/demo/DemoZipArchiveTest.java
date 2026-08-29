package com.retrocrawler.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.Journal;
import com.retrocrawler.core.Model;
import com.retrocrawler.core.RetroCrawler;
import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;
import com.retrocrawler.core.archive.InMemoryRepository;
import com.retrocrawler.core.archive.ReindexScope;
import com.retrocrawler.core.archive.source.ZipArchiveSource;
import com.retrocrawler.demo.gear.MyKnownGear;

class DemoZipArchiveTest {

	@Test
	void crawlsThePackagedRetroPcZipWithoutExtraction() throws Exception {
		final URL resource = DemoZipArchiveTest.class.getResource("/rc_demo_archives/retro_pc.zip");
		final Path zip = Path.of(resource.toURI());
		final Model model = Model.from(DemoModels.RETRO_PC.getBasePackage());
		final ArchiveDescriptor archive = ArchiveDescriptor.of(ArchiveId.of("retro_pc_demo_zip"), zip);
		final RetroCrawler crawler = RetroCrawler.builder().model(model).repository(new InMemoryRepository())
				.archive(archive, new ZipArchiveSource()).build();

		final List<MyKnownGear> gear = crawler.crawl(new Journal(), ReindexScope.all()).query(MyKnownGear.class).pull()
				.gear();

		final String folder = "ATI VGA Wonder 16 [SN VHK 144994] [200326]";
		final MyKnownGear graphicsCard = gear.stream().filter(candidate -> folder.equals(candidate.getFolderName()))
				.findFirst().orElseThrow();
		assertEquals(ARI.of("retro_pc_demo", archive.id(), Path.of("Graphics Cards", folder, "front.jpeg")),
				graphicsCard.getPicFront().orElseThrow());
	}
}
