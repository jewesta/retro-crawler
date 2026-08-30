package com.retrocrawler.server;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.retrocrawler.core.Locations;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;

/** External configuration of RetroCrawler's conventional local locations. */
@ConfigurationProperties("retro-crawler")
public record LocationsProperties(RepositoryLocation repository, List<ArchiveLocation> archives) {

	public LocationsProperties {
		Objects.requireNonNull(repository, "retro-crawler.repository must be configured");
		if (archives == null || archives.isEmpty()) {
			throw new IllegalArgumentException("retro-crawler.archives must contain at least one archive");
		}
		archives = List.copyOf(archives);
	}

	Locations toLocations() {
		return new Locations(repository.root(), archives.stream().map(ArchiveLocation::descriptor).toList());
	}

	/** The writable root of the rebuildable clue repository. */
	public record RepositoryLocation(Path root) {

		public RepositoryLocation {
			Objects.requireNonNull(root, "retro-crawler.repository.root must be configured");
		}
	}

	/** One named source archive belonging to the selected collection. */
	public record ArchiveLocation(String id, String name, Path root) {

		public ArchiveLocation {
			id = requireText(id, "id");
			name = requireText(name, "name");
			Objects.requireNonNull(root, "retro-crawler.archives[].root must be configured");
		}

		ArchiveDescriptor descriptor() {
			return new ArchiveDescriptor(ArchiveId.of(id), name, root);
		}

		private static String requireText(final String value, final String property) {
			if (value == null || value.isBlank()) {
				throw new IllegalArgumentException("retro-crawler.archives[]." + property + " must be configured");
			}
			return value.trim();
		}
	}
}
