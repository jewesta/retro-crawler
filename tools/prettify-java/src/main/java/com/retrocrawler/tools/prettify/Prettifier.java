package com.retrocrawler.tools.prettify;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.retrocrawler.tools.prettify.EclipseJavaFormatter.FormattedSource;
import com.retrocrawler.tools.prettify.MavenReactor.Module;
import com.retrocrawler.tools.prettify.RewriteCleaner.ResultOverlay;

final class Prettifier {

	record PendingWrite(Path file, String content) {
	}

	record Plan(Path repo, List<Module> modules, List<Path> sourceFiles, List<Path> cleanupChangedFiles,
			List<PendingWrite> pendingWrites) {

		Plan {
			modules = List.copyOf(modules);
			sourceFiles = List.copyOf(sourceFiles);
			cleanupChangedFiles = List.copyOf(cleanupChangedFiles);
			pendingWrites = List.copyOf(pendingWrites);
		}

		List<Path> changedFiles() {
			return pendingWrites.stream().map(PendingWrite::file).toList();
		}

		void persist() {
			try {
				for (final PendingWrite pendingWrite : pendingWrites) {
					Files.writeString(pendingWrite.file(), pendingWrite.content(), UTF_8);
				}
			} catch (final IOException e) {
				throw new PrettifyException("Failed to write Java prettify changes.", e);
			}
		}

	}

	private static final Path FORMATTER_PROFILE = Path.of("tools", "prettify-java", "formatting-rules.xml");

	Plan createPlan(final Path repo, final boolean prepare, final List<String> requestedModules,
			final List<Path> requestedFiles) {
		final MavenReactor reactor = new MavenReactor();
		final List<Module> reactorModules = reactor.read(repo);
		final SourceSelection selection = SourceSelection.collect(repo, reactorModules, requestedModules,
				requestedFiles);
		final MavenRunner maven = new MavenRunner();
		if (prepare) {
			System.out.println("Preparing Maven modules for type-aware cleanup...");
			maven.prepare(repo, selection.modules());
		}
		System.out.println("Resolving Maven classpaths...");
		final Map<Module, List<Path>> classpaths = maven.resolveClasspaths(repo, selection.modules(), reactorModules,
				selection.targeted());
		final Map<Path, String> originals = readSources(selection.files());

		System.out.println("Applying OpenRewrite cleanup...");
		final ResultOverlay cleanup = new RewriteCleaner().clean(repo, selection, classpaths);
		System.out.println("Applying Eclipse JDT formatting...");
		final EclipseJavaFormatter formatter = new EclipseJavaFormatter(repo.resolve(FORMATTER_PROFILE));
		final List<PendingWrite> pendingWrites = new ArrayList<>();
		for (final Path file : selection.files()) {
			final String original = originals.get(file);
			final String cleaned = cleanup.sources().getOrDefault(file, original);
			final FormattedSource formatted = formatter.format(cleaned, original);
			if (formatted.changed()) {
				pendingWrites.add(new PendingWrite(file, formatted.content()));
			}
		}
		return new Plan(repo, selection.modules(), selection.files(), cleanup.changedFiles(), pendingWrites);
	}

	private static Map<Path, String> readSources(final List<Path> files) {
		try {
			final Map<Path, String> sources = new LinkedHashMap<>();
			for (final Path file : files) {
				sources.put(file, Files.readString(file, UTF_8));
			}
			return Map.copyOf(sources);
		} catch (final IOException e) {
			throw new PrettifyException("Failed to read Java sources.", e);
		}
	}

}
