package com.retrocrawler.core;

import java.util.Objects;

import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.source.ArchiveSource;

/** One composed logical archive and its source provider. */
record ArchiveBinding(ArchiveDescriptor descriptor, ArchiveSource source) {

	ArchiveBinding {
		Objects.requireNonNull(descriptor, "descriptor");
		Objects.requireNonNull(source, "source");
		if (descriptor.paths().isEmpty()) {
			throw new IllegalArgumentException("Archive requires at least one root: " + descriptor.id());
		}
	}
}
