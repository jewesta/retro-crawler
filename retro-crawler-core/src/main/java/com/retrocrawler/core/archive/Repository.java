package com.retrocrawler.core.archive;

import java.util.Optional;

import com.retrocrawler.core.archive.clues.Archive;

public interface Repository {

	void stowaway(Archive archive);

	Optional<Archive> retrieve(ArchiveId id);

}
