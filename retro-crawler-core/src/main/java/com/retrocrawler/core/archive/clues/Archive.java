package com.retrocrawler.core.archive.clues;

import java.nio.file.Path;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.retrocrawler.core.archive.ArchiveId;

/**
 * One extracted, model-independent clue archive.
 * <p>
 * The {@link #collectionId()} and {@link #id()} anchor stable resource
 * identities. The tree below {@link #root()} is purely relative structure. The
 * {@link #basePath()} anchors it to the deployment location it was dug from and
 * is rebound when that location changes.
 * <p>
 * {@code version} is serialized first as a format contract: a repository must
 * be able to read the stored cache version without decoding a version-dependent
 * shape, and reading it should not require scanning a potentially large clue
 * tree first. Remaining properties may be ordered freely.
 */
@JsonPropertyOrder("version")
public final class Archive {

	@JsonProperty("version")
	private final ArchiveVersion version;

	@JsonProperty("collectionId")
	private final String collectionId;

	@JsonProperty("id")
	private final ArchiveId id;

	@JsonProperty("basePath")
	private final String basePath;

	@JsonProperty("root")
	private final ArchiveNode root;

	@JsonCreator
	protected Archive(@JsonProperty("version") final ArchiveVersion version,
			@JsonProperty("collectionId") final String collectionId, @JsonProperty("id") final ArchiveId id,
			@JsonProperty("basePath") final String basePath, @JsonProperty("root") final ArchiveNode root) {
		this.version = Objects.requireNonNull(version, "version");
		this.collectionId = Objects.requireNonNull(collectionId, "collectionId");
		if (collectionId.isBlank()) {
			throw new IllegalArgumentException("collectionId must not be blank.");
		}
		this.id = Objects.requireNonNull(id, "id");
		this.basePath = Objects.requireNonNull(basePath, "basePath");
		this.root = Objects.requireNonNull(root, "root");
	}

	public ArchiveId id() {
		return id;
	}

	public String collectionId() {
		return collectionId;
	}

	public ArchiveVersion version() {
		return version;
	}

	public String basePath() {
		return basePath;
	}

	public ArchiveNode root() {
		return root;
	}

	public static final Archive of(final String collectionId, final ArchiveId id, final Path basePath,
			final ArchiveNode root) {
		Objects.requireNonNull(basePath, "basePath");
		return new Archive(ArchiveVersion.CURRENT_IMPLEMENTATION_VERSION, collectionId, id, basePath.toString(), root);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[collectionId=" + collectionId() + ", id=" + id() + ", basePath="
				+ basePath() + "]";
	}

}
