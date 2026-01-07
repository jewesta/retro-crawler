package com.retrocrawler.core.archive.clues;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ArchiveNode {

	@JsonProperty("folder")
	private final String folder;

	@JsonProperty("artifact")
	private final Artifact artifact;

	@JsonProperty("children")
	private final List<ArchiveNode> children;

	@JsonCreator
	public ArchiveNode(@JsonProperty("folder") final String folder, @JsonProperty("artifact") final Artifact artifact,
			@JsonProperty("children") final List<ArchiveNode> children) {
		this.folder = Objects.requireNonNull(folder, "folder");
		this.artifact = artifact;
		this.children = children;
	}

	public Artifact getArtifact() {
		return artifact;
	}

	public List<ArchiveNode> getChildren() {
		return children;
	}

}
