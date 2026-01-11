package com.retrocrawler.app.cli;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import com.retrocrawler.core.GearArchive;
import com.retrocrawler.core.RetroCrawler;
import com.retrocrawler.core.RetroCrawlerFactory;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.util.Monitor;
import com.retrocrawler.demo.collection.DemoTypes;
import com.retrocrawler.demo.collection.DemoFiles;
import com.retrocrawler.demo.collection.gear.MyRetroGear;

public final class RetroCrawlerCli {

	private RetroCrawlerCli() {
		// no instances
	}

	public static void main(final String[] args) throws Exception {
		BannerPrinter.print("Command Line Demo");

		final Args parsed = Args.parse(args);

		final RetroCrawlerFactory factory = new RetroCrawlerFactory();
		final RetroCrawler crawler = factory.reflectOn(DemoTypes.RETRO_PC.getTypes());

		final Instant start = Instant.now();
		final Monitor monitor = new Monitor(msg -> {
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
		 * type annotated with @RetroArchive.
		 */
		final ArchiveDescriptor descriptor = crawler.getArchiveDescriptor();
		System.out.println("Creating local demo archive for " + descriptor.getName());
		DemoFiles.copyToWorkDirectory(descriptor);
		System.out.println("Local archive files created.");

		System.out.println("Reindex: " + parsed.reindex + " ("
				+ (parsed.reindex ? "Cache will be re-built" : "Cache will be used if present") + ")");

		System.out.println();
		final GearArchive<MyRetroGear> myRetroGear = crawler.crawlArchive(monitor, parsed.reindex, MyRetroGear.class);

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
		final boolean reindex;

		Args(final boolean reindex) {
			this.reindex = reindex;
		}

		static Args parse(final String[] args) {
			boolean reindex = false;
			for (final String a : args) {
				if ("--reindex".equalsIgnoreCase(a) || "-r".equalsIgnoreCase(a)) {
					reindex = true;
				}
			}
			return new Args(reindex);
		}
	}
}