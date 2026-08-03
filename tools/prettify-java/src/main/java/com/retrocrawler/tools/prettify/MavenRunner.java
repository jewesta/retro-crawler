package com.retrocrawler.tools.prettify;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.retrocrawler.tools.prettify.MavenReactor.Module;

final class MavenRunner {

	private static final String CLASSPATH_FILE = "prettify-java-classpath.txt";

	void prepare(final Path repo, final List<Module> modules) {
		if (modules.isEmpty()) {
			return;
		}
		run(repo, modules, List.of("-DskipTests", "process-test-classes"), "Maven preparation");
	}

	Map<Module, List<Path>> resolveClasspaths(final Path repo, final List<Module> selectedModules,
			final List<Module> reactorModules, final boolean targeted) {
		if (selectedModules.isEmpty()) {
			return Map.of();
		}
		run(repo, selectedModules,
				List.of("-Dmdep.outputFile=target/" + CLASSPATH_FILE, "-Dmdep.includeScope=test",
						"org.apache.maven.plugins:maven-dependency-plugin:3.9.0:build-classpath"),
				"Maven classpath resolution");

		final Map<Module, List<Path>> classpaths = new LinkedHashMap<>();
		for (final Module module : selectedModules) {
			final Set<Path> classpath = new LinkedHashSet<>();
			for (final Module reactorModule : reactorModules) {
				if (!reactorModule.equals(module) || targeted) {
					addDirectory(classpath, reactorModule.path().resolve("target/classes"));
					addDirectory(classpath, reactorModule.path().resolve("target/test-classes"));
				}
			}
			classpath.addAll(readDependencyClasspath(module));
			classpaths.put(module, List.copyOf(classpath));
		}
		return Map.copyOf(classpaths);
	}

	private static void run(final Path repo, final List<Module> modules, final List<String> goals,
			final String operation) {
		final List<String> arguments = new ArrayList<>();
		arguments.addAll(List.of("-q", "-B", "-ntp", "-f", repo.resolve("pom.xml").toString(), "-pl",
				modules.stream().map(Module::displayPath).reduce((left, right) -> left + "," + right).orElseThrow(),
				"-am"));
		arguments.addAll(goals);

		final List<String> command = mavenCommand(arguments);
		try {
			final Process process = new ProcessBuilder(command).directory(repo.toFile()).inheritIO().start();
			final int exitCode = process.waitFor();
			if (exitCode != 0) {
				throw new PrettifyException(operation + " failed with exit code " + exitCode + ".");
			}
		} catch (final IOException e) {
			throw new PrettifyException("Failed to start " + operation.toLowerCase() + ".", e);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new PrettifyException("Interrupted during " + operation.toLowerCase() + ".", e);
		}
	}

	private static List<String> mavenCommand(final List<String> arguments) {
		final List<String> command = new ArrayList<>();
		if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
			command.addAll(List.of("cmd.exe", "/d", "/c", "mvn.cmd"));
		} else {
			command.add("mvn");
		}
		command.addAll(arguments);
		return command;
	}

	private static List<Path> readDependencyClasspath(final Module module) {
		final Path classpathFile = module.path().resolve("target").resolve(CLASSPATH_FILE);
		if (!Files.isRegularFile(classpathFile)) {
			return List.of();
		}
		try {
			final String classpath = Files.readString(classpathFile).trim();
			if (classpath.isBlank()) {
				return List.of();
			}
			return List.of(classpath.split(File.pathSeparator)).stream().filter(entry -> !entry.isBlank()).map(Path::of)
					.map(path -> path.toAbsolutePath().normalize()).toList();
		} catch (final IOException e) {
			throw new PrettifyException("Failed to read Maven classpath for " + module.displayPath() + ".", e);
		}
	}

	private static void addDirectory(final Set<Path> classpath, final Path path) {
		if (Files.isDirectory(path)) {
			classpath.add(path.toAbsolutePath().normalize());
		}
	}

}
