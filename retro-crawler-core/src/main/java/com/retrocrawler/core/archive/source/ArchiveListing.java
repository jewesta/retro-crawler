package com.retrocrawler.core.archive.source;

import java.util.List;
import java.util.Objects;

/** One classified snapshot of a folder's direct archive entries. */
public record ArchiveListing(List<ArchiveFolder> folders, List<ArchiveFile> files) {

	public ArchiveListing {
		folders = List.copyOf(Objects.requireNonNull(folders, "folders"));
		files = List.copyOf(Objects.requireNonNull(files, "files"));
	}
}
