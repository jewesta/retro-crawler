package com.retrocrawler.tools.prettify;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

final class MavenReactor {

	record Module(Path repo, Path relativePath, String artifactId) {

		Module {
			repo = Objects.requireNonNull(repo, "repo").toAbsolutePath().normalize();
			relativePath = Objects.requireNonNull(relativePath, "relativePath").normalize();
			artifactId = Objects.requireNonNull(artifactId, "artifactId");
		}

		Path path() {
			return repo.resolve(relativePath).normalize();
		}

		boolean contains(final Path file) {
			return file.toAbsolutePath().normalize().startsWith(path());
		}

		String displayPath() {
			final String path = relativePath.toString().replace('\\', '/');
			return path.isBlank() ? "." : path;
		}

		boolean matches(final String requested) {
			return artifactId.equals(requested) || displayPath().equals(requested.replace('\\', '/'));
		}

	}

	List<Module> read(final Path repo) {
		final List<Module> modules = new ArrayList<>();
		readModule(repo, Path.of(""), modules, new HashSet<>());
		return List.copyOf(modules);
	}

	Module ownerOf(final Path file, final List<Module> modules) {
		return modules.stream().filter(module -> module.contains(file))
				.max(Comparator.comparingInt(module -> module.path().getNameCount()))
				.orElseThrow(() -> new PrettifyException("No Maven module contains " + file + "."));
	}

	List<Module> resolveRequested(final List<String> requested, final List<Module> modules) {
		final List<Module> resolved = new ArrayList<>();
		for (final String value : requested) {
			final List<Module> matches = modules.stream().filter(module -> module.matches(value)).toList();
			if (matches.isEmpty()) {
				throw new PrettifyException("Unknown Maven module: " + value + ".");
			}
			if (matches.size() > 1) {
				throw new PrettifyException("Ambiguous Maven module: " + value + ".");
			}
			resolved.add(matches.get(0));
		}
		return List.copyOf(resolved);
	}

	private void readModule(final Path repo, final Path relativePath, final List<Module> modules,
			final Set<Path> visitedPoms) {
		final Path pom = repo.resolve(relativePath).resolve("pom.xml").normalize();
		if (!Files.isRegularFile(pom) || !visitedPoms.add(pom)) {
			return;
		}

		try {
			final Element project = parseProject(pom);
			final String artifactId = directChildText(project, "artifactId");
			if (artifactId != null) {
				modules.add(new Module(repo, relativePath, artifactId));
			}

			final Element modulesElement = directChild(project, "modules");
			if (modulesElement == null) {
				return;
			}
			final NodeList moduleElements = modulesElement.getChildNodes();
			for (int i = 0; i < moduleElements.getLength(); i++) {
				final Node node = moduleElements.item(i);
				if (node instanceof final Element element && "module".equals(element.getLocalName())) {
					final String modulePath = element.getTextContent().trim();
					if (!modulePath.isBlank()) {
						readModule(repo, relativePath.resolve(modulePath), modules, visitedPoms);
					}
				}
			}
		} catch (final Exception e) {
			throw new PrettifyException("Failed to read Maven module " + pom + ".", e);
		}
	}

	private static Element parseProject(final Path pom) throws Exception {
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		try (InputStream inputStream = Files.newInputStream(pom)) {
			return factory.newDocumentBuilder().parse(inputStream).getDocumentElement();
		}
	}

	private static Element directChild(final Element parent, final String name) {
		final NodeList children = parent.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			final Node node = children.item(i);
			if (node instanceof final Element element && name.equals(element.getLocalName())) {
				return element;
			}
		}
		return null;
	}

	private static String directChildText(final Element parent, final String name) {
		final Element child = directChild(parent, name);
		return child == null ? null : child.getTextContent().trim();
	}

}
