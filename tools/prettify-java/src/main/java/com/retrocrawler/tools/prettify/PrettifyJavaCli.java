package com.retrocrawler.tools.prettify;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.retrocrawler.tools.prettify.Prettifier.Plan;

public final class PrettifyJavaCli {

	private enum Mode {
		ASSERT,
		APPLY
	}

	private static final class Arguments {

		private final List<String> modules = new ArrayList<>();

		private final List<Path> files = new ArrayList<>();

		private Mode mode = Mode.ASSERT;

		private boolean modeSet;

		private boolean prepare = true;

		private boolean help;

		private Path repo;

		void setMode(final Mode mode) {
			if (modeSet && this.mode != mode) {
				throw new PrettifyException("Use only one of --assert and --apply.");
			}
			this.mode = mode;
			modeSet = true;
		}

	}

	private PrettifyJavaCli() {
		// static utility class
	}

	public static void main(final String[] args) {
		System.exit(run(args));
	}

	static int run(final String[] args) {
		try {
			final Arguments arguments = parse(args);
			if (arguments.help) {
				printUsage();
				return 0;
			}
			final Path repo = resolveRepo(arguments.repo);
			final List<Path> files = arguments.files.stream().map(file -> resolveFile(repo, file)).toList();
			final Plan plan = new Prettifier().createPlan(repo, arguments.prepare, arguments.modules, files);
			printReport(plan, arguments.prepare);
			if (arguments.mode == Mode.APPLY) {
				plan.persist();
				System.out.println("Java prettify applied " + plan.changedFiles().size() + " file(s).");
				return 0;
			}
			if (plan.changedFiles().isEmpty()) {
				System.out.println("Java prettify assertion passed.");
				return 0;
			}
			System.err.println(
					"Java prettify assertion failed: " + plan.changedFiles().size() + " file(s) would change.");
			return 1;
		} catch (final PrettifyException | IllegalArgumentException e) {
			System.err.println("Java prettify failed: " + failureMessage(e));
			return 2;
		}
	}

	private static String failureMessage(final RuntimeException failure) {
		Throwable cause = failure;
		while (cause.getCause() != null) {
			cause = cause.getCause();
		}
		if (cause == failure || cause.getMessage() == null) {
			return failure.getMessage();
		}
		return failure.getMessage() + " " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
	}

	private static Arguments parse(final String[] args) {
		final Arguments arguments = new Arguments();
		for (int i = 0; i < args.length; i++) {
			final String argument = args[i];
			switch (argument) {
			case "--help", "-h" -> arguments.help = true;
			case "--assert" -> arguments.setMode(Mode.ASSERT);
			case "--apply" -> arguments.setMode(Mode.APPLY);
			case "--no-prepare" -> arguments.prepare = false;
			case "--module" -> arguments.modules.add(requiredValue(args, ++i, argument));
			case "--repo" -> arguments.repo = Path.of(requiredValue(args, ++i, argument));
			default -> {
				if (argument.startsWith("--")) {
					throw new PrettifyException("Unknown option: " + argument + ".");
				}
				arguments.files.add(Path.of(argument));
			}
			}
		}
		return arguments;
	}

	private static String requiredValue(final String[] args, final int index, final String option) {
		if (index >= args.length || args[index].startsWith("--")) {
			throw new PrettifyException("Missing value for " + option + ".");
		}
		return args[index];
	}

	private static Path resolveRepo(final Path configuredRepo) {
		if (configuredRepo != null) {
			return validateRepo(configuredRepo.toAbsolutePath().normalize());
		}
		Path candidate = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
		while (candidate != null) {
			if (Files.isDirectory(candidate.resolve(".git")) && Files.isRegularFile(candidate.resolve("pom.xml"))) {
				return candidate;
			}
			candidate = candidate.getParent();
		}
		throw new PrettifyException("Could not locate a Maven repository root from the current directory.");
	}

	private static Path validateRepo(final Path repo) {
		if (!Files.isDirectory(repo.resolve(".git")) || !Files.isRegularFile(repo.resolve("pom.xml"))) {
			throw new PrettifyException("Repository root must contain .git and pom.xml: " + repo + ".");
		}
		return repo;
	}

	private static Path resolveFile(final Path repo, final Path file) {
		if (file.isAbsolute()) {
			return file.normalize();
		}
		final Path fromWorkingDirectory = Path.of(System.getProperty("user.dir")).resolve(file).normalize();
		return Files.exists(fromWorkingDirectory) ? fromWorkingDirectory : repo.resolve(file).normalize();
	}

	private static void printReport(final Plan plan, final boolean prepared) {
		System.out.println("Repository: " + plan.repo());
		System.out.println("Prepared Maven reactor: " + prepared);
		System.out.println("Selected modules: " + plan.modules().size());
		System.out.println("Source files: " + plan.sourceFiles().size());
		System.out.println("Cleanup changed files: " + plan.cleanupChangedFiles().size());
		System.out.println("Changed files: " + plan.changedFiles().size());
		for (final Path changedFile : plan.changedFiles()) {
			System.out.println(" - " + plan.repo().relativize(changedFile));
		}
	}

	private static void printUsage() {
		System.out.println("Usage:");
		System.out.println("  prettify-java [--assert|--apply] [options] [java-file...]");
		System.out.println();
		System.out.println("Options:");
		System.out.println("  --assert             Check only. This is the default.");
		System.out.println("  --apply              Write cleanup and formatting changes.");
		System.out.println("  --module <module>    Restrict to a Maven module path or artifactId. Repeatable.");
		System.out.println("  --no-prepare         Skip Maven compilation before resolving types.");
		System.out.println("  --repo <repo-root>   Repository root. Normally supplied by the launcher.");
		System.out.println("  --help               Show this help.");
	}

}
