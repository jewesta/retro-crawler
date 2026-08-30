package com.retrocrawler.core;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;

/**
 * The conventional local locations of a clue repository and its source
 * archives.
 * <p>
 * Supplying locations to a {@link RetroCrawler.Builder} selects the JSON clue
 * repository and filesystem archive source. Applications needing another
 * repository or source use the builder's explicit methods instead.
 */
public record Locations(Path repositoryRoot, List<ArchiveDescriptor> archives) {

	public Locations {
		Objects.requireNonNull(repositoryRoot, "repositoryRoot");
		Objects.requireNonNull(archives, "archives");
		if (archives.isEmpty()) {
			throw new IllegalArgumentException("At least one archive location must be configured.");
		}
		archives = List.copyOf(archives);

		final Set<ArchiveId> archiveIds = new HashSet<>();
		for (final ArchiveDescriptor archive : archives) {
			if (!archiveIds.add(archive.id())) {
				throw new IllegalArgumentException("Archive location is already configured: " + archive.id());
			}
		}
	}
}
