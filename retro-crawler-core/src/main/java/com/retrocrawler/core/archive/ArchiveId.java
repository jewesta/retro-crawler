package com.retrocrawler.core.archive;

import com.retrocrawler.core.util.AbstractId;

public final class ArchiveId extends AbstractId<String> {

	protected ArchiveId(final String id) {
		super(id);
	}

	public static final ArchiveId of(final String id) {
		return new ArchiveId(id);
	}

}
