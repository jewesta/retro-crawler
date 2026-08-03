package com.retrocrawler.core.stash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.clues.ArchiveNode;
import com.retrocrawler.core.archive.clues.Bucket;

class StashStatsTest {

	@Test
	void describesAnEmptyStash() {
		final StashStats stats = StashStats.from(new Stash<>(List.of()));

		assertEquals(0, stats.bucketCount());
		assertEquals(0, stats.rootCount());
		assertEquals(0, stats.nodeCount());
		assertEquals(0, stats.leafCount());
		assertEquals(0, stats.maximumDepth());
		assertEquals(Map.of(), stats.gearByType());
	}

	@Test
	void describesBucketAndGearTreeStructure() {
		final GearNode<Object> deepest = new GearNode<>(23, List.of());
		final GearNode<Object> child = new GearNode<>(new Right.Gear(), List.of(deepest));
		final GearNode<Object> firstRoot = new GearNode<>(new Left.Gear(), List.of(child));
		final GearNode<Object> secondRoot = new GearNode<>("loose", List.of());
		final Stash<Object> stash = new Stash<>(
				List.of(new GearBucket<>(bucket("first"), List.of(firstRoot, secondRoot)),
						new GearBucket<>(bucket("second"), List.of())));

		final StashStats stats = StashStats.from(stash);

		assertEquals(2, stats.bucketCount());
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

	private static Bucket bucket(final String folder) {
		return Bucket.of(Path.of(folder), new ArchiveNode(folder, null, List.of()));
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
