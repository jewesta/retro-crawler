package com.retrocrawler.core.archive;

import java.util.List;
import java.util.Objects;

import com.retrocrawler.core.archive.source.ArchiveFile;
import com.retrocrawler.core.archive.source.ArchiveFolder;

/**
 * One classified snapshot of a folder's direct entries.
 */
record FolderListing(List<ArchiveFolder> folders, List<ArchiveFile> files) {

	FolderListing {
		folders = List.copyOf(Objects.requireNonNull(folders, "folders"));
		files = List.copyOf(Objects.requireNonNull(files, "files"));
	}

}
