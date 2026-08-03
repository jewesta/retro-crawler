package com.retrocrawler.app.cli;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import com.retrocrawler.core.GearArchive;
import com.retrocrawler.core.Model;
import com.retrocrawler.core.RetroCrawler;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.JsonFileRepository;
import com.retrocrawler.core.archive.ReindexScope;
import com.retrocrawler.core.progress.Progressor;
import com.retrocrawler.demo.DemoFiles;
import com.retrocrawler.demo.DemoModels;
import com.retrocrawler.demo.gear.MyRetroGear;

public final class RetroCrawlerCli {

	private RetroCrawlerCli() {
		// no instances
	}

	public static void main(final String[] args) throws Exception {
		BannerPrinter.print("Command Line Demo");

		final Args parsed = Args.parse(args);

		final Model model = Model.from(DemoModels.RETRO_PC.getBasePackage());
		final RetroCrawler crawler = RetroCrawler.builder().model(model).repository(new JsonFileRepository()).build();

		final Instant start = Instant.now();
		final Progressor progressor = Progressor.reportingMessages(msg -> {
			System.out.println(msg);
		});

		/*
		 * To run a demo we need a folder structure with some retro gear documentation /
		 * pictures / etc. RetroCrawler can scan and convert to POJO instances. The demo
		 * module provides these files, but since the demo module is a JAR file they are
		 * essentially inside a ZIP file. To make them usable for RetroCrawler which
		 * works with the actual local file system we first need to extract them from
		 * the JAR file and put them inside a local directory.
		 * 
		 * The local folder will be exactly what's annotated as the location in the demo
		 * type annotated with @RetroCollection. Currently /rc_demo_archives.
		 */
		final ArchiveDescriptor descriptor = crawler.archiveDescriptor();
		System.out.println("Creating local demo archive for " + descriptor.name());
		DemoFiles.copyToWorkDirectory(descriptor);
		System.out.println("Local archive files created.");

		System.out.println("Reindex scope: " + parsed.reindexScope);

		System.out.println();
		final GearArchive<MyRetroGear> myRetroGear = crawler.crawlArchive(progressor, parsed.reindexScope,
				MyRetroGear.class);

		final Duration dur = Duration.between(start, Instant.now());

		System.out.println();
		System.out.println("Crawling took: " + dur.toMillis() + " ms");
		System.out.println();

		AsciiTreePrinter.printArchive(myRetroGear);

		System.out.println();
		System.out.println("Statistics");
		final ArchiveStats stats = ArchiveStats.compute(myRetroGear);
		stats.printToStdout();
	}

	static final class Args {
		final ReindexScope reindexScope;

		Args(final ReindexScope reindexScope) {
			this.reindexScope = reindexScope;
		}

		static Args parse(final String[] args) {
			ReindexScope reindexScope = ReindexScope.none();
			for (final String a : args) {
				if ("--reindex".equalsIgnoreCase(a) || "-r".equalsIgnoreCase(a)) {
					reindexScope = ReindexScope.all();
				}
			}
			return new Args(reindexScope);
		}
	}
}
