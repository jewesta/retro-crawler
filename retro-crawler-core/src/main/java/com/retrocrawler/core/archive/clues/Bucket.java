package com.retrocrawler.core.archive.clues;

import java.nio.file.Path;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Bucket {

	@JsonProperty("basePath")
	private final String basePath;

	@JsonProperty("root")
	private final ArchiveNode root;

	@JsonCreator
	protected Bucket(@JsonProperty("basePath") final String basePath, @JsonProperty("root") final ArchiveNode root) {
		this.basePath = Objects.requireNonNull(basePath, "basePath");
		this.root = Objects.requireNonNull(root, "root");
	}

	public String getBasePath() {
		return basePath;
	}

	public ArchiveNode getRoot() {
		return root;
	}

	public static final Bucket of(final Path basePath, final ArchiveNode root) {
		return new Bucket(basePath.toString(), root);
	}

}
