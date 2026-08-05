package com.retrocrawler.core.archive.clues;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One folder in the persisted archive tree.
 * <p>
 * {@link #crawledAt()} records the operation that most recently crawled this
 * complete subtree. Partial reindexing replaces the selected subtree with newly
 * timestamped nodes while retaining the timestamps of its ancestors and
 * untouched siblings.
 */
public class ArchiveNode {

	@JsonProperty("folder")
	private final String folder;

	@JsonProperty("crawledAt")
	private final Instant crawledAt;

	@JsonProperty("artifact")
	private final Artifact artifact;

	@JsonProperty("children")
	private final List<ArchiveNode> children;

	@JsonCreator
	public ArchiveNode(@JsonProperty("folder") final String folder, @JsonProperty("crawledAt") final Instant crawledAt,
			@JsonProperty("artifact") final Artifact artifact,
			@JsonProperty("children") final List<ArchiveNode> children) {
		this.folder = Objects.requireNonNull(folder, "folder");
		this.crawledAt = Objects.requireNonNull(crawledAt, "crawledAt");
		this.artifact = artifact;
		this.children = children;
	}

	/** Creates a standalone node timestamped at construction time. */
	public ArchiveNode(final String folder, final Artifact artifact, final List<ArchiveNode> children) {
		this(folder, Instant.now(), artifact, children);
	}

	public Artifact artifact() {
		return artifact;
	}

	public String folder() {
		return folder;
	}

	public Instant crawledAt() {
		return crawledAt;
	}

	public List<ArchiveNode> children() {
		return children;
	}

}
