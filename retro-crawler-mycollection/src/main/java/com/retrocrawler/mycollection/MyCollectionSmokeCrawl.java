package com.retrocrawler.mycollection;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.retrocrawler.core.Model;
import com.retrocrawler.core.RetroCrawler;
import com.retrocrawler.core.archive.ArchiveRoots;
import com.retrocrawler.core.archive.JsonFileRepository;
import com.retrocrawler.core.util.CrawlProgress;
import com.retrocrawler.core.util.Monitor;
import com.retrocrawler.mycollection.gear.MyGear;

/**
 * Explicit local smoke-crawl entry point. Archive roots and the private clue
 * cache remain external runtime configuration.
 */
public final class MyCollectionSmokeCrawl {

	private MyCollectionSmokeCrawl() {
		// No instances.
	}

	public static void main(final String[] arguments) throws Exception {
		if (arguments.length < 2 || arguments.length > 3) {
			throw new IllegalArgumentException(
					"Expected arguments: <archive-roots-file> <private-cache-directory> [--reindex|--reuse-cache]");
		}

		final ArchiveRoots roots = ArchiveRoots.load(Path.of(arguments[0]));
		final Path cacheDirectory = Path.of(arguments[1]);
		final boolean reindex = reindex(arguments);
		final Model model = Model.from(AttributeNames.class.getPackageName(), roots);
		final RetroCrawler crawler = RetroCrawler.builder().model(model)
				.repository(new JsonFileRepository(cacheDirectory)).build();
		final Monitor monitor = Monitor.observing(new CompactProgressPrinter());

		final List<MyGear> gear = crawler.crawlGear(monitor, reindex, MyGear.class);
		printSummary(gear);
	}

	private static boolean reindex(final String[] arguments) {
		if (arguments.length == 2 || "--reindex".equals(arguments[2])) {
			return true;
		}
		if ("--reuse-cache".equals(arguments[2])) {
			return false;
		}
		throw new IllegalArgumentException("Unknown crawl mode: " + arguments[2]);
	}

	private static void printSummary(final List<MyGear> gear) {
		final Map<String, Long> types = gear.stream()
				.collect(Collectors.groupingBy(value -> value.getClass().getSimpleName(), TreeMap::new,
						Collectors.counting()));
		final long withRetroId = gear.stream().filter(value -> value.getRetroId().isPresent()).count();
		final long withDescription = gear.stream().filter(value -> value.getDescription().isPresent()).count();
		final long withImages = gear.stream().filter(MyCollectionSmokeCrawl::hasImage).count();
		final long withFloppyImages = gear.stream().filter(value -> !value.getFloppyImages().isEmpty()).count();
		final long onlyImages = gear.stream().filter(MyCollectionSmokeCrawl::hasOnlyImages).count();

		System.out.println("RC_RESULT\tgear=" + gear.size() + "\ttypes=" + types + "\tretroIds=" + withRetroId
				+ "\tmissingRetroIds=" + (gear.size() - withRetroId) + "\tdescriptions=" + withDescription
				+ "\timages=" + withImages + "\tfloppyImages=" + withFloppyImages + "\timagesOnly=" + onlyImages);
	}

	private static boolean hasImage(final MyGear gear) {
		return gear.getAngledImage().isPresent() || gear.getFrontImage().isPresent()
				|| gear.getBackImage().isPresent();
	}

	private static boolean hasOnlyImages(final MyGear gear) {
		return hasImage(gear) && hasNoOtherCollectionClues(gear);
	}

	private static boolean hasNoOtherCollectionClues(final MyGear gear) {
		return gear.getRetroId().isEmpty() && gear.getBus().isEmpty() && gear.getTitle().isEmpty()
				&& gear.getDescription().isEmpty() && gear.getFloppyImages().isEmpty()
				&& gear.getFloppyImageIds().isEmpty()
				&& gear.getAttributes().keySet().stream().allMatch(key -> key.startsWith("@"));
	}

	private static final class CompactProgressPrinter implements Consumer<CrawlProgress> {

		private CrawlProgress.Phase previousPhase;

		private long previousPercentageBucket = Long.MIN_VALUE;

		@Override
		public void accept(final CrawlProgress progress) {
			final boolean phaseChanged = progress.phase() != previousPhase;
			final long percentageBucket = percentageBucket(progress);
			final boolean completed = progress.isDeterminate() && progress.completed() == progress.total();
			if (!phaseChanged && !completed && percentageBucket == previousPercentageBucket) {
				return;
			}

			final String amount = progress.isDeterminate()
					? progress.completed() + "/" + progress.total()
					: "-";
			final String precision = progress.isDeterminate()
					? progress.approximate() ? "APPROXIMATE" : "EXACT"
					: "INDETERMINATE";
			final String message = progress.message().replace('\n', ' ').replace('\r', ' ');
			System.out.println("RC_PROGRESS\t" + progress.phase() + "\t" + amount + "\t" + precision + "\t"
					+ message);

			previousPhase = progress.phase();
			previousPercentageBucket = percentageBucket;
		}

		private static long percentageBucket(final CrawlProgress progress) {
			if (!progress.isDeterminate() || progress.total() == 0) {
				return Long.MIN_VALUE;
			}
			return progress.completed() * 100 / progress.total() / 5;
		}
	}
}
