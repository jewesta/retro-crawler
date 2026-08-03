package com.retrocrawler.core.archive;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.retrocrawler.core.archive.clues.Archive;

/**
 * Keeps extracted clue archives in memory for the lifetime of this repository.
 * <p>
 * This repository is useful when an application wants to avoid filesystem
 * storage and does not need its extracted archives to survive a restart.
 * Instances are safe to share between threads.
 */
public class InMemoryRepository implements Repository {

	private final ConcurrentMap<ArchiveId, Archive> archives = new ConcurrentHashMap<>();

	@Override
	public void stowaway(final Archive archive) {
		Objects.requireNonNull(archive, "archive");
		archives.put(archive.id(), archive);
	}

	@Override
	public Optional<Archive> retrieve(final ArchiveId id) {
		Objects.requireNonNull(id, "id");
		return Optional.ofNullable(archives.get(id));
	}

}
