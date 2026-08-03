package com.retrocrawler.tools.prettify;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.RecipeRun;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

import com.retrocrawler.tools.prettify.MavenReactor.Module;

final class RewriteCleaner {

	record ResultOverlay(Map<Path, String> sources, List<Path> changedFiles) {

		ResultOverlay {
			sources = Map.copyOf(sources);
			changedFiles = List.copyOf(changedFiles);
		}

	}

	ResultOverlay clean(final Path repo, final SourceSelection selection,
			final Map<Module, List<Path>> moduleClasspaths) {
		if (selection.files().isEmpty()) {
			return new ResultOverlay(Map.of(), List.of());
		}

		final InMemoryExecutionContext context = new InMemoryExecutionContext(throwable -> {
			throw new PrettifyException("OpenRewrite cleanup failed.", throwable);
		});
		final List<SourceFile> parsedSources = parseByModule(repo, selection, moduleClasspaths, context);
		final Recipe recipe = new JavaCleanup();
		final RecipeRun recipeRun = recipe.run(new InMemoryLargeSourceSet(parsedSources), context);
		return collectResults(repo, selection.files(), recipeRun.getChangeset().getAllResults());
	}

	private static List<SourceFile> parseByModule(final Path repo, final SourceSelection selection,
			final Map<Module, List<Path>> moduleClasspaths, final InMemoryExecutionContext context) {
		final List<SourceFile> parsedSources = new ArrayList<>();
		for (final Module module : selection.modules()) {
			final List<Path> moduleFiles = selection.files().stream().filter(module::contains).toList();
			if (moduleFiles.isEmpty()) {
				continue;
			}
			try {
				parsedSources
						.addAll(JavaParser.fromJavaVersion().classpath(moduleClasspaths.getOrDefault(module, List.of()))
								.build().parse(moduleFiles, repo, context).toList());
			} catch (final RuntimeException e) {
				throw new PrettifyException("OpenRewrite could not parse Maven module " + module.displayPath() + ".",
						e);
			}
		}
		return List.copyOf(parsedSources);
	}

	private static ResultOverlay collectResults(final Path repo, final List<Path> writableFiles,
			final List<Result> results) {
		final Set<Path> writable = new LinkedHashSet<>(writableFiles);
		final Map<Path, String> sources = new LinkedHashMap<>();
		for (final Result result : results) {
			if (result.getAfter() == null) {
				throw new PrettifyException(
						"OpenRewrite attempted to delete " + result.getBefore().getSourcePath() + ".");
			}
			final Path file = repo.resolve(result.getAfter().getSourcePath()).normalize();
			final String after = result.getAfter().printAll();
			if (writable.contains(file) && !after.equals(result.getBefore().printAll())) {
				sources.put(file, after);
			}
		}
		return new ResultOverlay(sources, sources.keySet().stream().sorted(Comparator.naturalOrder()).toList());
	}

}
