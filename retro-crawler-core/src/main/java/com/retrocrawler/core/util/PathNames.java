package com.retrocrawler.core.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class PathNames {

	private PathNames() {
		// static utility class
	}

	public static String abbreviatePathName(final String path) {
		Objects.requireNonNull(path, "path");

		final boolean absolute = path.startsWith("/");
		final String[] parts = path.split("/");

		if (parts.length <= 2) {
			return path;
		}

		final StringBuilder out = new StringBuilder();

		if (absolute) {
			out.append('/');
		}

		for (int i = absolute ? 1 : 0; i < parts.length - 2; i++) {
			if (!parts[i].isEmpty()) {
				out.append("./");
			}
		}

		out.append(parts[parts.length - 2]).append('/').append(parts[parts.length - 1]);

		return out.toString();
	}

	/**
	 * For future reference.
	 */
	public static long size(final Path path) throws IOException {
		final AtomicLong count = new AtomicLong();
		Files.walkFileTree(path, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
				count.incrementAndGet();
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) {
				count.incrementAndGet();
				return FileVisitResult.CONTINUE;
			}
		});
		return count.get();
	}

}
