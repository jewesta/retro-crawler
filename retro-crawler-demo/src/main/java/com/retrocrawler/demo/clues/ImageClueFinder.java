package com.retrocrawler.demo.clues;

import java.nio.file.Path;
import java.util.Collection;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.ClueAccumulator;
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.core.archive.clues.FileNameClueFinder;
import com.retrocrawler.demo.AttributeNames;

public class ImageClueFinder implements FileNameClueFinder {

	// Paths are artifact-relative addresses already classified as files by the provider.
	@Override
	public Clues find(final Collection<Path> files) {
		final ClueAccumulator clues = Clues.accumulator();
		files.stream().forEach(path -> {
			final String fileName = path.getFileName().toString();
			final String resourcePath = FileNameClueFinder.portablePath(path);
			if (fileName.equalsIgnoreCase("front.jpeg")) {
				clues.add(Clue.of(AttributeNames.PIC_FRONT, resourcePath));
			} else if (fileName.equalsIgnoreCase("back.jpeg")) {
				clues.add(Clue.of(AttributeNames.PIC_BACK, resourcePath));
			}
		});
		return clues.clues();
	}

}
