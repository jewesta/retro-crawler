package com.retrocrawler.demo.clues;

import com.retrocrawler.core.archive.clues.ArchiveFolderView;
import com.retrocrawler.core.archive.clues.ClueAccumulator;
import com.retrocrawler.core.archive.clues.ClueFinder;
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.demo.AttributeNames;

public class ImageClueFinder implements ClueFinder {

	@Override
	public Clues find(final ArchiveFolderView folder) {
		final ClueAccumulator clues = Clues.accumulator();
		folder.files().forEach(file -> {
			final String fileName = file.name();
			if (fileName.equalsIgnoreCase("front.jpeg")) {
				clues.add(file.clue(AttributeNames.PIC_FRONT, fileName));
			} else if (fileName.equalsIgnoreCase("back.jpeg")) {
				clues.add(file.clue(AttributeNames.PIC_BACK, fileName));
			}
		});
		return clues.clues();
	}

}
