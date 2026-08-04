package com.retrocrawler.core.archive;

import java.util.Objects;

import com.retrocrawler.core.archive.source.ArchiveFolder;
import com.retrocrawler.core.archive.source.ArchiveSession;

record ArchiveDigTarget(ArchiveSession session, ArchiveFolder root, ArchiveFolder folder) {

	ArchiveDigTarget {
		Objects.requireNonNull(session, "session");
		Objects.requireNonNull(root, "root");
		Objects.requireNonNull(folder, "folder");
		if (!folder.path().normalize().startsWith(root.path().normalize())) {
			throw new IllegalArgumentException(
					"Expected archive path '" + folder.path() + "' to be below root '" + root.path() + "'.");
		}
	}
}
