package com.retrocrawler.mycollection;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.retrocrawler.core.DuplicateRetroIdException;
import com.retrocrawler.core.Model;
import com.retrocrawler.core.RetroCrawler;
import com.retrocrawler.core.archive.ArchiveRoots;
import com.retrocrawler.core.archive.JsonFileRepository;
import com.retrocrawler.core.archive.ReindexScope;
import com.retrocrawler.core.progress.FixedStepProgressMonitor;
import com.retrocrawler.core.progress.ProgressSnapshot;
import com.retrocrawler.core.progress.Progressor;
import com.retrocrawler.mycollection.gear.Diskette;
import com.retrocrawler.mycollection.gear.HardDiskDrive;
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
		if (arguments.length < 2) {
			throw new IllegalArgumentException(
					"Expected arguments: <archive-roots-file> <private-cache-directory> "
							+ "[--reindex|--reuse-cache|--reindex-subtree <path>...]");
		}

		final ArchiveRoots roots = ArchiveRoots.load(Path.of(arguments[0]));
		final Path cacheDirectory = Path.of(arguments[1]);
		final ReindexScope reindexScope = reindexScope(arguments);
		final Model model = Model.from(AttributeNames.class.getPackageName(), roots);
		final RetroCrawler crawler = RetroCrawler.builder().model(model)
				.repository(new JsonFileRepository(cacheDirectory)).build();
		final Progressor progressor = Progressor.observing(new CompactProgressPrinter());

		try {
			final List<MyGear> gear = crawler.crawlGear(progressor, reindexScope, MyGear.class);
			Files.deleteIfExists(cacheDirectory.resolve("duplicate-retro-ids.txt"));
			printSummary(gear);
		} catch (final DuplicateRetroIdException failure) {
			final Path report = writeDuplicateReport(cacheDirectory, failure);
			final long occurrences = failure.getDuplicates().values().stream().mapToLong(List::size).sum();
			System.out.println("RC_VALIDATION\tduplicateRetroIds=" + failure.getDuplicates().size()
					+ "\toccurrences=" + occurrences + "\treport=" + report);
			throw new IllegalStateException("Duplicate Retro IDs detected; see private report: " + report);
		}
	}

	private static ReindexScope reindexScope(final String[] arguments) {
		if (arguments.length == 2) {
			return ReindexScope.all();
		}
		if ("--reindex".equals(arguments[2]) && arguments.length == 3) {
			return ReindexScope.all();
		}
		if ("--reuse-cache".equals(arguments[2]) && arguments.length == 3) {
			return ReindexScope.none();
		}
		if ("--reindex-subtree".equals(arguments[2]) && arguments.length >= 4) {
			final List<Path> paths = Arrays.stream(arguments, 3, arguments.length).map(Path::of).toList();
			return ReindexScope.subtrees(paths);
		}
		throw new IllegalArgumentException(
				"Expected --reindex, --reuse-cache, or --reindex-subtree followed by at least one path.");
	}

	private static void printSummary(final List<MyGear> gear) {
		final Map<String, Long> types = gear.stream()
				.collect(Collectors.groupingBy(value -> value.getClass().getSimpleName(), TreeMap::new,
						Collectors.counting()));
		final long withRetroId = gear.stream().filter(value -> value.getRetroId().isPresent()).count();
		final long withDescription = gear.stream().filter(value -> value.getDescription().isPresent()).count();
		final long withColors = gear.stream().filter(value -> !value.getColors().isEmpty()).count();
		final long withChipDesignations = gear.stream()
				.filter(value -> !value.getChipDesignations().isEmpty()).count();
		final long withCondition = gear.stream().filter(value -> value.getCondition().isPresent()).count();
		final long withDamage = gear.stream().filter(value -> !value.getDamageKinds().isEmpty()).count();
		final long withDateMarkings = gear.stream().filter(value -> value.getDateMarking().isPresent()).count();
		final long withHealth = gear.stream().filter(value -> value.getHealth().isPresent()).count();
		final long withOriginalPackaging = gear.stream().filter(MyGear::hasOriginalPackaging).count();
		final long withSealState = gear.stream().filter(value -> value.getSealState().isPresent()).count();
		final long withImages = gear.stream().filter(MyCollectionSmokeCrawl::hasImage).count();
		final long withFloppyImages = gear.stream().filter(value -> !value.getFloppyImages().isEmpty()).count();
		final long withLanguages = gear.stream().filter(value -> !value.getLanguages().isEmpty()).count();
		final long withLengths = gear.stream().filter(value -> value.getLength().isPresent()).count();
		final long withLotMembership = gear.stream().filter(value -> !value.getLot().isEmpty()).count();
		final long withNintendoGameBoyCartridgeCodes = gear.stream()
				.filter(value -> !value.getNintendoGameBoyCartridgeCodes().isEmpty()).count();
		final long withPlayStationPortableDiscIds = gear.stream()
				.filter(value -> !value.getPlayStationPortableDiscIds().isEmpty()).count();
		final long withSegaGameGearCartridgeCodes = gear.stream()
				.filter(value -> !value.getSegaGameGearCartridgeCodes().isEmpty()).count();
		final long withRegions = gear.stream().filter(value -> !value.getRegions().isEmpty()).count();
		final long onlyImages = gear.stream().filter(MyCollectionSmokeCrawl::hasOnlyImages).count();

		System.out.println("RC_RESULT\tgear=" + gear.size() + "\ttypes=" + types + "\tretroIds=" + withRetroId
				+ "\tmissingRetroIds=" + (gear.size() - withRetroId) + "\tdescriptions=" + withDescription
				+ "\timages=" + withImages + "\tfloppyImages=" + withFloppyImages + "\tcolors=" + withColors
				+ "\tchipDesignations=" + withChipDesignations
				+ "\tconditions=" + withCondition + "\thealth=" + withHealth + "\tdamage=" + withDamage
				+ "\tdateMarkings=" + withDateMarkings
				+ "\toriginalPackaging=" + withOriginalPackaging + "\tsealStates=" + withSealState
				+ "\tlanguages=" + withLanguages + "\tregions=" + withRegions
				+ "\tlengths=" + withLengths
				+ "\tgameBoyCartridgeCodes=" + withNintendoGameBoyCartridgeCodes
				+ "\tgameGearCartridgeCodes=" + withSegaGameGearCartridgeCodes
				+ "\tpspDiscIds=" + withPlayStationPortableDiscIds
				+ "\tlotMemberships=" + withLotMembership + "\timagesOnly=" + onlyImages);
	}

	private static Path writeDuplicateReport(final Path cacheDirectory, final DuplicateRetroIdException failure)
			throws java.io.IOException {
		final Path report = cacheDirectory.resolve("duplicate-retro-ids.txt");
		final long occurrences = failure.getDuplicates().values().stream().mapToLong(List::size).sum();
		final StringBuilder contents = new StringBuilder()
				.append("Duplicate Retro IDs\n")
				.append("===================\n\n")
				.append("Values: ").append(failure.getDuplicates().size()).append('\n')
				.append("Occurrences: ").append(occurrences).append("\n\n");

		failure.getDuplicates().entrySet().stream()
				.sorted(Map.Entry.comparingByKey((left, right) -> left.toString().compareTo(right.toString())))
				.forEach(entry -> {
					contents.append(entry.getKey()).append('\n');
					entry.getValue().forEach(path -> contents.append("  ").append(path).append('\n'));
					contents.append('\n');
				});

		Files.writeString(report, contents);
		try {
			Files.setPosixFilePermissions(report, PosixFilePermissions.fromString("rw-------"));
		} catch (final UnsupportedOperationException ignored) {
			// The private cache directory remains the privacy boundary on non-POSIX systems.
		}
		return report;
	}

	private static boolean hasImage(final MyGear gear) {
		return gear.getAngledImage().isPresent() || gear.getFrontImage().isPresent()
				|| gear.getBackImage().isPresent();
	}

	private static boolean hasOnlyImages(final MyGear gear) {
		return hasImage(gear) && hasNoOtherCollectionClues(gear);
	}

	private static boolean hasNoOtherCollectionClues(final MyGear gear) {
		return hasNoTypeSpecificFacts(gear) && gear.getRetroId().isEmpty() && gear.getExpansionBuses().isEmpty()
				&& gear.getTitle().isEmpty() && gear.getColors().isEmpty() && gear.getChipDesignations().isEmpty()
				&& gear.getCondition().isEmpty()
				&& gear.getDamageKinds().isEmpty() && gear.getDateMarking().isEmpty()
				&& gear.getGearKind().isEmpty() && gear.getLength().isEmpty()
				&& gear.getPackagingOrigin().isEmpty()
				&& gear.getCapacity().isEmpty() && gear.getDescription().isEmpty() && gear.getIsbn().isEmpty()
				&& gear.getLanguages().isEmpty() && gear.getRegions().isEmpty()
				&& gear.getDestiny().isEmpty() && gear.getFccId().isEmpty() && gear.getHealth().isEmpty()
				&& gear.getMacAddress().isEmpty() && gear.getSerialNumber().isEmpty()
				&& gear.getNintendoGameBoyCartridgeCodes().isEmpty()
				&& gear.getPlayStationPortableDiscIds().isEmpty()
				&& gear.getSegaGameGearCartridgeCodes().isEmpty()
				&& gear.getPrice().isEmpty() && gear.getLot().isEmpty() && gear.getLotPrice().isEmpty()
				&& gear.getSource().isEmpty()
				&& gear.getTested().isEmpty()
				&& gear.getFloppyImages().isEmpty() && gear.getFloppyImageIds().isEmpty()
				&& gear.getMemoryAccessTimes().isEmpty()
				&& gear.getMemoryFeatures().isEmpty() && gear.getMemoryFormFactors().isEmpty()
				&& gear.getMemoryStandards().isEmpty() && gear.getComputerFormFactors().isEmpty()
				&& gear.getPower().isEmpty() && gear.getRamSet().isEmpty() && gear.getDocumentIds().isEmpty()
				&& gear.getSealState().isEmpty()
				&& gear.getScreenSize().isEmpty() && gear.getVersion().isEmpty()
				&& gear.getTheRetroWebId().isEmpty() && gear.getVideoConnectors().isEmpty()
				&& gear.getAttributes().keySet().stream().allMatch(key -> key.startsWith("@"));
	}

	private static boolean hasNoTypeSpecificFacts(final MyGear gear) {
		final boolean noDisketteFacts = !(gear instanceof final Diskette diskette)
				|| diskette.getTrackDensities().isEmpty()
						&& diskette.getFloppyDiskFormats().isEmpty()
						&& diskette.getFormFactor().isEmpty();
		final boolean noHardDiskDriveFacts = !(gear instanceof final HardDiskDrive hardDiskDrive)
				|| hardDiskDrive.getFormFactor().isEmpty();
		return noDisketteFacts && noHardDiskDriveFacts;
	}

	private static final class CompactProgressPrinter extends FixedStepProgressMonitor {

		private CompactProgressPrinter() {
			super(20);
		}

		@Override
		protected void onProgressStep(final long maximumStep, final long currentStep,
				final ProgressSnapshot progress) {
			final String amount = progress.isDeterminate()
					? progress.completed() + "/" + progress.total()
					: "-";
			final String precision = progress.accuracy().toString();
			final String message = progress.message().replace('\n', ' ').replace('\r', ' ');
			System.out.println("RC_PROGRESS\t" + progress.stage() + "\t" + amount + "\t" + precision + "\t"
					+ message);
		}
	}
}
