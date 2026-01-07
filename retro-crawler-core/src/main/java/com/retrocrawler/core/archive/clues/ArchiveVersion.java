package com.retrocrawler.core.archive.clues;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.retrocrawler.core.util.AbstractId;

public final class ArchiveVersion extends AbstractId<Integer> {

	public static final ArchiveVersion CURRENT_IMPLEMENTATION_VERSION = new ArchiveVersion(1);

	@JsonCreator
	protected ArchiveVersion(final int version) {
		super(version);
	}

	public static final ArchiveVersion of(final int version) {
		if (version < 1 || version > CURRENT_IMPLEMENTATION_VERSION.get().intValue()) {
			throw new IllegalArgumentException(
					"Expected version to be >= 1 and <= " + CURRENT_IMPLEMENTATION_VERSION + " but got " + version);
		}
		return new ArchiveVersion(version);
	}
}
