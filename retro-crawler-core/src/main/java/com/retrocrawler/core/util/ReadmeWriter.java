package com.retrocrawler.core.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ReadmeWriter {

	public static final String README_TXT = "readme.txt";

	public static final String NOTICE_TEMPORARY_FOLDER_DO_NOT_COMMIT = "Automatically created temporary folder. Can be deled any time. Do not commit.";

	private ReadmeWriter() {
		// utility class
	}

	public static Path writeReadmeTemporary(final Path targetDirectory) throws IOException {
		return writeReadme(targetDirectory, NOTICE_TEMPORARY_FOLDER_DO_NOT_COMMIT);
	}

	public static Path writeReadme(final Path targetDirectory, final String content) throws IOException {
		if (!Files.exists(targetDirectory)) {
			throw new IOException("Target directory does not exist: " + targetDirectory);
		}
		if (!Files.isDirectory(targetDirectory)) {
			throw new IOException("Target path is not a directory: " + targetDirectory);
		}

		final Path readme = targetDirectory.resolve(README_TXT);
		Files.writeString(readme, content, StandardCharsets.UTF_8);

		return readme;
	}

}