package com.retrocrawler.core.archive.clues;

import java.nio.file.Path;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.retrocrawler.core.archive.ArchiveId;

/**
 * One extracted, model-independent clue archive.
 * <p>
 * The tree below {@link #root()} is purely relative structure. The
 * {@link #basePath()} anchors it to the deployment location it was dug from and
 * is rebound when that location changes.
 */
public final class Archive {

	@JsonProperty("version")
	private final ArchiveVersion version;

	@JsonProperty("id")
	private final ArchiveId id;

	@JsonProperty("basePath")
	private final String basePath;

	@JsonProperty("root")
	private final ArchiveNode root;

	@JsonCreator
	protected Archive(@JsonProperty("version") final ArchiveVersion version, @JsonProperty("id") final ArchiveId id,
			@JsonProperty("basePath") final String basePath, @JsonProperty("root") final ArchiveNode root) {
		this.version = Objects.requireNonNull(version, "version");
		this.id = Objects.requireNonNull(id, "id");
		this.basePath = Objects.requireNonNull(basePath, "basePath");
		this.root = Objects.requireNonNull(root, "root");
	}

	public ArchiveId id() {
		return id;
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

	public static final Archive of(final ArchiveId id, final Path basePath, final ArchiveNode root) {
		Objects.requireNonNull(basePath, "basePath");
		return new Archive(ArchiveVersion.CURRENT_IMPLEMENTATION_VERSION, id, basePath.toString(), root);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[id=" + id() + ", basePath=" + basePath() + "]";
	}

}
