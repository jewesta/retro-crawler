package com.retrocrawler.core.archive.clues;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One folder in the persisted archive tree.
 * <p>
 * {@link #crawlStartedAt()} identifies the operation that most recently crawled
 * this complete subtree. {@link #observedAt()} records when this particular
 * folder had been fully inspected, including its children. Partial reindexing
 * replaces the selected subtree with newly timestamped nodes while retaining
 * the timestamps of its ancestors and untouched siblings.
 */
public class ArchiveNode {

	@JsonProperty("folder")
	private final String folder;

	@JsonProperty("crawlStartedAt")
	private final Instant crawlStartedAt;

	@JsonProperty("observedAt")
	private final Instant observedAt;

	@JsonProperty("artifact")
	private final Artifact artifact;

	@JsonProperty("children")
	private final List<ArchiveNode> children;

	@JsonCreator
	public ArchiveNode(@JsonProperty("folder") final String folder,
			@JsonProperty("crawlStartedAt") final Instant crawlStartedAt,
			@JsonProperty("observedAt") final Instant observedAt, @JsonProperty("artifact") final Artifact artifact,
			@JsonProperty("children") final List<ArchiveNode> children) {
		this.folder = Objects.requireNonNull(folder, "folder");
		this.crawlStartedAt = Objects.requireNonNull(crawlStartedAt, "crawlStartedAt");
		this.observedAt = Objects.requireNonNull(observedAt, "observedAt");
		this.artifact = artifact;
		this.children = children;
	}

	/** Creates a node whose observation time equals its crawl start. */
	public ArchiveNode(final String folder, final Instant crawlStartedAt, final Artifact artifact,
			final List<ArchiveNode> children) {
		this(folder, crawlStartedAt, crawlStartedAt, artifact, children);
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

	public Instant crawlStartedAt() {
		return crawlStartedAt;
	}

	public Instant observedAt() {
		return observedAt;
	}

	public List<ArchiveNode> children() {
		return children;
	}

}
