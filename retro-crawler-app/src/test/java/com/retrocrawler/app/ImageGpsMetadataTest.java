package com.retrocrawler.app;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;

class ImageGpsMetadataTest {

	@Test
	void noJpegMayContainGpsMetadata() throws IOException {
		final Path moduleRoot = Path.of("").toAbsolutePath();

		final List<Path> offendingFiles;
		try (Stream<Path> paths = Files.walk(moduleRoot)) {
			offendingFiles = paths.filter(Files::isRegularFile).filter(this::isJpeg).filter(this::containsGpsMetadata)
					.collect(Collectors.toList());
		}

		if (!offendingFiles.isEmpty()) {
			final String message = offendingFiles.stream().map(moduleRoot::relativize).map(Path::toString)
					.collect(Collectors.joining(System.lineSeparator(),
							"JPEG files containing GPS metadata detected:" + System.lineSeparator(), ""));

			fail(message);
		}
	}

	private boolean isJpeg(final Path path) {
		final String name = path.getFileName().toString().toLowerCase();
		return name.endsWith(".jpg") || name.endsWith(".jpeg");
	}

	private boolean containsGpsMetadata(final Path path) {
		try {
			final Metadata metadata = ImageMetadataReader.readMetadata(path.toFile());
			final GpsDirectory gps = metadata.getFirstDirectoryOfType(GpsDirectory.class);
			return gps != null && gps.getGeoLocation() != null;
		} catch (final Exception e) {
			/*
			 * If the file cannot be parsed, we assume it is not a valid JPEG and therefore
			 * ignore it.
			 */
			return false;
		}
	}
}