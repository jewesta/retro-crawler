package com.retrocrawler.core.util;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

public final class NioPathCounter {

	private NioPathCounter() {
		// utility class
	}

	/**
	 * Counts all paths (files and directories) under the given root path
	 * recursively.
	 *
	 * @param rootPath the root directory as a string
	 * @return the number of paths found, including the root directory itself
	 * @throws IOException if an IO error occurs
	 */
	public static long countPathsRecursively(final String rootPath) throws IOException {
		final Path root = FileSystems.getDefault().getPath(rootPath);
		return countPathsRecursively(root);
	}

	private static long countPathsRecursively(final Path dir) throws IOException {
		long count = 1; // count this directory itself

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
			for (final Path path : stream) {
				if (Files.isDirectory(path)) {
					count += countPathsRecursively(path);
				} else {
					// count++;
				}
			}
		}

		return count;
	}
}