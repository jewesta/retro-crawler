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
import com.retrocrawler.demo.collection.ResourceArchive;
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

		final ArchiveDescriptor descriptor = crawler.getArchiveDescriptor();
		System.out.println("Creating local demo archive for " + descriptor.getName());
		for (final Path path : descriptor.getPaths()) {
			System.out.println("Extracting " + path.toString() + "...");
			ResourceArchive.ensureLocalArchive(path);
		}
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