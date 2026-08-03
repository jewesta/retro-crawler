package com.retrocrawler.tools.prettify;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

final class EclipseJavaFormatter {

	record FormattedSource(String content, boolean changed) {
	}

	private final CodeFormatter formatter;

	EclipseJavaFormatter(final Path profile) {
		try {
			final Map<String, String> options = new LinkedHashMap<>(loadOptions(profile));
			options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_21);
			options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_21);
			options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_21);
			formatter = ToolFactory.createCodeFormatter(options);
		} catch (final Exception e) {
			throw new PrettifyException("Failed to load Eclipse formatter profile " + profile + ".", e);
		}
	}

	FormattedSource format(final String source, final String original) {
		try {
			final TextEdit edit = formatter.format(CodeFormatter.K_COMPILATION_UNIT | CodeFormatter.F_INCLUDE_COMMENTS,
					source, 0, source.length(), 0, "\n");
			if (edit == null) {
				throw new PrettifyException("Eclipse JDT could not format a Java compilation unit.");
			}
			final Document document = new Document(source);
			edit.apply(document);
			final String formatted = localizeLineEndings(document.get(), original);
			return new FormattedSource(formatted, !formatted.equals(original));
		} catch (final PrettifyException e) {
			throw e;
		} catch (final Exception e) {
			throw new PrettifyException("Eclipse JDT failed to format a Java compilation unit.", e);
		}
	}

	private static Map<String, String> loadOptions(final Path profile) throws Exception {
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		try (InputStream inputStream = Files.newInputStream(profile)) {
			final org.w3c.dom.Document document = factory.newDocumentBuilder().parse(inputStream);
			final NodeList settings = document.getElementsByTagName("setting");
			final Map<String, String> options = new LinkedHashMap<>();
			for (int i = 0; i < settings.getLength(); i++) {
				final Element setting = (Element) settings.item(i);
				options.put(setting.getAttribute("id"), setting.getAttribute("value"));
			}
			return Map.copyOf(options);
		}
	}

	private static String localizeLineEndings(final String formatted, final String original) {
		return original.contains("\r\n") ? formatted.replace("\n", "\r\n") : formatted;
	}

}
