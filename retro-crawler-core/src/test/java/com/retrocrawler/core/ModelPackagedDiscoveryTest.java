package com.retrocrawler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModelPackagedDiscoveryTest {

	private static final String PACKAGE_NAME = "com.retrocrawler.packagedfixture";

	@TempDir
	private Path temporaryDirectory;

	@Test
	void discoversModelInJarFromContextClassLoader() throws IOException {
		final Path classesDirectory = compileModelTypes();
		final Path jar = createJar(classesDirectory);
		final ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();

		try (URLClassLoader jarClassLoader = new URLClassLoader(new URL[] { jar.toUri().toURL() },
				originalContextClassLoader)) {
			Thread.currentThread().setContextClassLoader(jarClassLoader);

			final Model model = Model.from(PACKAGE_NAME);

			assertEquals("packaged_model", model.getArchiveDescriptor().getId().get());
		} finally {
			Thread.currentThread().setContextClassLoader(originalContextClassLoader);
		}
	}

	private Path compileModelTypes() throws IOException {
		final Path sourceDirectory = temporaryDirectory.resolve("source");
		final Path classesDirectory = temporaryDirectory.resolve("classes");
		final Path packageDirectory = sourceDirectory.resolve(PACKAGE_NAME.replace('.', '/'));
		final Path archiveSource = packageDirectory.resolve("PackagedArchive.java");
		final Path gearSource = packageDirectory.resolve("PackagedGear.java");
		Files.createDirectories(packageDirectory);
		Files.createDirectories(classesDirectory);
		Files.writeString(archiveSource, """
				package com.retrocrawler.packagedfixture;

				@com.retrocrawler.core.annotation.RetroCollection(
						id = "packaged_model",
						locations = "/unused")
				@com.retrocrawler.core.annotation.RetroClues(
						fromFolderName = com.retrocrawler.core.discovery.fixture.EmptyClueFinder.class)
				public class PackagedArchive {
				}
				""");
		Files.writeString(gearSource, """
				package com.retrocrawler.packagedfixture;

				@com.retrocrawler.core.annotation.RetroGear(
						com.retrocrawler.core.gear.matcher.AnyGearMatcher.class)
				public class PackagedGear {

					@com.retrocrawler.core.annotation.RetroAnyAttribute
					private final java.util.Map<String, com.retrocrawler.core.util.RetroAttribute> attributes =
							new java.util.HashMap<>();

					public PackagedGear() {
					}
				}
				""");

		final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		assertNotNull(compiler, "The packaged-discovery test requires a JDK.");
		final int result = compiler.run(null, null, null, "--release", "21", "-classpath",
				System.getProperty("java.class.path"), "-d", classesDirectory.toString(), archiveSource.toString(),
				gearSource.toString());
		assertEquals(0, result, "The packaged model fixture must compile.");
		return classesDirectory;
	}

	private Path createJar(final Path classesDirectory) throws IOException {
		final Path jar = temporaryDirectory.resolve("model.jar");
		try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar));
				var paths = Files.walk(classesDirectory)) {
			for (final Path path : paths.filter(Files::isRegularFile).toList()) {
				final String entryName = classesDirectory.relativize(path).toString().replace('\\', '/');
				output.putNextEntry(new JarEntry(entryName));
				Files.copy(path, output);
				output.closeEntry();
			}
		}
		assertTrue(Files.size(jar) > 0);
		return jar;
	}
}
