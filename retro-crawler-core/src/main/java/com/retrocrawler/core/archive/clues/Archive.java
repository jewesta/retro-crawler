package com.retrocrawler.core.archive.clues;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.retrocrawler.core.archive.ArchiveId;

public final class Archive {

	@JsonProperty("version")
	private final ArchiveVersion version;

	@JsonProperty("id")
	private final ArchiveId id;

	@JsonProperty("buckets")
	private final List<Bucket> buckets;

	@JsonCreator
	protected Archive(@JsonProperty("version") final ArchiveVersion version, @JsonProperty("id") final ArchiveId id,
			@JsonProperty("buckets") final List<Bucket> buckets) {
		this.version = Objects.requireNonNull(version, "version");
		this.id = Objects.requireNonNull(id, "id");
		this.buckets = Objects.requireNonNull(buckets, "buckets");
	}

	public ArchiveId getId() {
		return id;
	}

	public List<Bucket> getBuckets() {
		return buckets;
	}

	public static final Archive of(final ArchiveId id, final List<Bucket> buckets) {
		return new Archive(ArchiveVersion.CURRENT_IMPLEMENTATION_VERSION, id, buckets);
	}

}
