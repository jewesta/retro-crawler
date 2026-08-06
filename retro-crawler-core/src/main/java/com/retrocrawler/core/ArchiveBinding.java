package com.retrocrawler.core;

import java.util.Objects;

import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.source.ArchiveSource;

/** One composed archive and the provider that exposes its root. */
record ArchiveBinding(ArchiveDescriptor descriptor, ArchiveSource source) {

	ArchiveBinding {
		Objects.requireNonNull(descriptor, "descriptor");
		Objects.requireNonNull(source, "source");
	}
}
