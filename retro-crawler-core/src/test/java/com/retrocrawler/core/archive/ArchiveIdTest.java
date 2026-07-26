package com.retrocrawler.core.archive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ArchiveIdTest {

	@Test
	void hasValueSemantics() {
		final ArchiveId first = ArchiveId.of("collection");
		final ArchiveId second = ArchiveId.of("collection");
		final ArchiveId other = ArchiveId.of("other");

		assertEquals(first, second);
		assertEquals(first.hashCode(), second.hashCode());
		assertNotEquals(first, other);
	}

	@Test
	void canBeUsedAsMapKey() {
		final Map<ArchiveId, String> archives = new HashMap<>();
		archives.put(ArchiveId.of("collection"), "stored");

		assertEquals("stored", archives.get(ArchiveId.of("collection")));
	}

}
