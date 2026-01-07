package com.retrocrawler.app.collection.clues;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.retrocrawler.app.collection.AttributeNames;
import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.FileNameClueFinder;

public class ImageClueFinder implements FileNameClueFinder {

	// Path objects are guaranteed to represent files
	@Override
	public Set<Clue> find(final Collection<Path> files) {
		final Set<Clue> clues = new HashSet<>();
		files.stream().forEach(path -> {
			final String fileName = path.getFileName().toString();
			final String fullPathName = path.toString();
			if (fileName.equalsIgnoreCase("front.jpeg")) {
				clues.add(Clue.of(AttributeNames.PIC_FRONT, fullPathName));
			} else if (fileName.equalsIgnoreCase("back.jpeg")) {
				clues.add(Clue.of(AttributeNames.PIC_BACK, fullPathName));
			}
		});
		return clues;
	}

}
