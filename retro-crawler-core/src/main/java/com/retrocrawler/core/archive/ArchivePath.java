package com.retrocrawler.core.archive;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record ArchivePath(Path path, List<Path> children) {

	public ArchivePath {
		Objects.requireNonNull(path, "path");
		Objects.requireNonNull(children, "children");
	}

}