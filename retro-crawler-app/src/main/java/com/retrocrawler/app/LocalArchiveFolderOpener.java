package com.retrocrawler.app;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

final class LocalArchiveFolderOpener {

	@FunctionalInterface
	interface OpenAction {

		void open(Path path) throws IOException;
	}

	private final OpenAction openAction;

	LocalArchiveFolderOpener() {
		this(LocalArchiveFolderOpener::openOnDesktop);
	}

	LocalArchiveFolderOpener(final OpenAction openAction) {
		this.openAction = Objects.requireNonNull(openAction, "openAction");
	}

	void open(final Path sourcePath, final Path archiveRoot) throws IOException {
		Objects.requireNonNull(sourcePath, "sourcePath");
		Objects.requireNonNull(archiveRoot, "archiveRoot");

		final Path target = requireDirectory(sourcePath);
		if (!target.startsWith(requireDirectory(archiveRoot))) {
			throw new IllegalArgumentException("Refusing to open a folder outside the configured archive: " + target);
		}

		openAction.open(target);
	}

	private static Path requireDirectory(final Path path) throws IOException {
		final Path realPath = Objects.requireNonNull(path, "path").toRealPath();
		if (!Files.isDirectory(realPath)) {
			throw new IOException("Expected a folder but got: " + realPath);
		}
		return realPath;
	}

	private static void openOnDesktop(final Path path) throws IOException {
		if (GraphicsEnvironment.isHeadless() || !Desktop.isDesktopSupported()) {
			throw new IOException("Opening local folders is not supported in this environment.");
		}
		final Desktop desktop = Desktop.getDesktop();
		if (!desktop.isSupported(Desktop.Action.OPEN)) {
			throw new IOException("Opening local folders is not supported by this desktop.");
		}
		desktop.open(path.toFile());
	}
}
