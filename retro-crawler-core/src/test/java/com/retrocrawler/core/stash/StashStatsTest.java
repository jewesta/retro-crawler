package com.retrocrawler.core.stash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;

class StashStatsTest {

	@Test
	void describesAnEmptyStash() {
		final StashStats stats = new Stash(List.of()).stats();

		assertEquals(0, stats.archiveCount());
		assertEquals(0, stats.rootCount());
		assertEquals(0, stats.nodeCount());
		assertEquals(0, stats.leafCount());
		assertEquals(0, stats.maximumDepth());
		assertEquals(Map.of(), stats.gearByType());
	}

	@Test
	void describesArchiveAndGearTreeStructure() {
		final GearNode<Object> deepest = new GearNode<>(23, source("deepest"), List.of());
		final GearNode<Object> child = new GearNode<>(new Right.Gear(), source("child"), List.of(deepest));
		final GearNode<Object> firstRoot = new GearNode<>(new Left.Gear(), source("first"), List.of(child));
		final GearNode<Object> secondRoot = new GearNode<>("loose", source("second"), List.of());
		final Stash stash = new Stash(List.of(new ArchiveGear<>(archive("first"), List.of(firstRoot, secondRoot)),
				new ArchiveGear<>(archive("second"), List.of())));

		final StashStats stats = stash.stats();

		assertEquals(2, stats.archiveCount());
		assertEquals(2, stats.rootCount());
		assertEquals(4, stats.nodeCount());
		assertEquals(2, stats.leafCount());
		assertEquals(3, stats.maximumDepth());
		assertEquals(Map.of(Left.Gear.class, 1L, Right.Gear.class, 1L, Integer.class, 1L, String.class, 1L),
				stats.gearByType());
	}

	@Test
	void rejectsNullStash() {
		assertThrows(NullPointerException.class, () -> StashStats.from(null));
	}

	private static ArchiveDescriptor archive(final String name) {
		return ArchiveDescriptor.of(ArchiveId.of(name), Path.of(name));
	}

	private static ARI source(final String path) {
		return ARI.of("collection", ArchiveId.of("first"), Path.of(path));
	}

	private static final class Left {

		private static final class Gear {
		}
	}

	private static final class Right {

		private static final class Gear {
		}
	}
}
