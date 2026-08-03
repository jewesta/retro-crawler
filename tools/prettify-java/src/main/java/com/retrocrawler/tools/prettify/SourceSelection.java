package com.retrocrawler.tools.prettify;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.retrocrawler.tools.prettify.MavenReactor.Module;

record SourceSelection(List<Module> modules, List<Path> files, boolean targeted) {

	SourceSelection {
		modules = List.copyOf(modules);
		files = List.copyOf(files);
	}

	static SourceSelection collect(final Path repo, final List<Module> reactorModules,
			final List<String> requestedModules, final List<Path> requestedFiles) {
		final MavenReactor reactor = new MavenReactor();
		final List<Module> moduleFilters = reactor.resolveRequested(requestedModules, reactorModules);
		final boolean targeted = !requestedFiles.isEmpty();
		final List<Path> candidates = targeted ? validateRequested(repo, requestedFiles) : trackedJavaFiles(repo);
		final Set<Module> selectedModules = new LinkedHashSet<>();
		final List<Path> selectedFiles = new ArrayList<>();

		for (final Path file : candidates.stream().sorted().toList()) {
			final Module owner = reactor.ownerOf(file, reactorModules);
			if (!moduleFilters.isEmpty() && moduleFilters.stream().noneMatch(module -> module.contains(file))) {
				if (targeted) {
					throw new PrettifyException(
							"Java source is outside the selected Maven module(s): " + repo.relativize(file) + ".");
				}
				continue;
			}
			selectedModules.add(owner);
			selectedFiles.add(file);
		}

		return new SourceSelection(selectedModules.stream().sorted(Comparator.comparing(Module::displayPath)).toList(),
				selectedFiles, targeted);
	}

	private static List<Path> validateRequested(final Path repo, final List<Path> requestedFiles) {
		final Set<Path> validated = new LinkedHashSet<>();
		for (final Path requestedFile : requestedFiles) {
			final Path file = requestedFile.toAbsolutePath().normalize();
			if (!file.startsWith(repo)) {
				throw new PrettifyException("Java source is outside the repository: " + requestedFile + ".");
			}
			if (!file.toString().endsWith(".java")) {
				throw new PrettifyException("Source argument does not end with .java: " + requestedFile + ".");
			}
			if (!Files.isRegularFile(file)) {
				throw new PrettifyException("Java source does not exist: " + requestedFile + ".");
			}
			if (containsPathElement(repo.relativize(file), "target")) {
				throw new PrettifyException("Java source is in Maven build output: " + requestedFile + ".");
			}
			validated.add(file);
		}
		return List.copyOf(validated);
	}

	private static List<Path> trackedJavaFiles(final Path repo) {
		try {
			final Process process = new ProcessBuilder("git", "ls-files", "-z", "--", "*.java").directory(repo.toFile())
					.redirectErrorStream(true).start();
			final ByteArrayOutputStream output = new ByteArrayOutputStream();
			process.getInputStream().transferTo(output);
			final int exitCode = process.waitFor();
			if (exitCode != 0) {
				throw new PrettifyException(
						"git ls-files failed with exit code " + exitCode + ": " + output.toString(UTF_8));
			}
			return parseNulSeparatedPaths(repo, output.toByteArray());
		} catch (final IOException e) {
			throw new PrettifyException("Failed to list tracked Java sources.", e);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new PrettifyException("Interrupted while listing tracked Java sources.", e);
		}
	}

	private static List<Path> parseNulSeparatedPaths(final Path repo, final byte[] output) {
		final List<Path> paths = new ArrayList<>();
		int start = 0;
		for (int i = 0; i < output.length; i++) {
			if (output[i] != 0) {
				continue;
			}
			if (i > start) {
				final Path path = repo.resolve(new String(output, start, i - start, UTF_8)).normalize();
				if (Files.isRegularFile(path)) {
					paths.add(path);
				}
			}
			start = i + 1;
		}
		return List.copyOf(paths);
	}

	private static boolean containsPathElement(final Path path, final String name) {
		for (final Path element : path) {
			if (name.equals(element.toString())) {
				return true;
			}
		}
		return false;
	}

}
