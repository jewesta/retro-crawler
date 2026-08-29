package com.retrocrawler.core.stash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.retrocrawler.core.archive.ARI;
import com.retrocrawler.core.archive.ArchiveDescriptor;
import com.retrocrawler.core.archive.ArchiveId;

class StashQueryTest {

	private static final ArchiveId FIRST_ID = ArchiveId.of("first");

	private static final ArchiveId SECOND_ID = ArchiveId.of("second");

	@Test
	void pullsEverythingAsAnObjectBatchWithoutChangingTheNaturalHierarchy() {
		final Parent parent = new Parent("parent", true);
		final Child child = new Child("child", true);
		final Stash stash = stash(new GearNode<>(parent, source(FIRST_ID, "parent"),
				List.of(new GearNode<>(child, source(FIRST_ID, "parent/child"), List.of()))));

		final Batch<Object> result = stash.pull();

		assertEquals(List.of(parent, child), result.gear());
		assertEquals(parent, result.archives().getFirst().roots().getFirst().gear());
		assertEquals(child, result.archives().getFirst().roots().getFirst().children().getFirst().gear());
	}

	@Test
	void typesFiltersAndLiftsMatchingDescendantsAtPullTime() {
		final Parent excludedParent = new Parent("excluded", false);
		final Child liftedChild = new Child("lifted", true);
		final Child excludedChild = new Child("excluded child", false);
		final GearNode<Object> root = new GearNode<>(excludedParent, source(FIRST_ID, "parent"),
				List.of(new GearNode<>(liftedChild, source(FIRST_ID, "parent/lifted"), List.of()),
						new GearNode<>(excludedChild, source(FIRST_ID, "parent/excluded"), List.of())));

		final Batch<BaseGear> result = stash(root).query(BaseGear.class).where(BaseGear::working).pull();

		assertEquals(List.of(liftedChild), result.gear());
		assertEquals(liftedChild, result.archives().getFirst().roots().getFirst().gear());
		assertEquals(source(FIRST_ID, "parent/lifted"), result.archives().getFirst().roots().getFirst().source());
	}

	@Test
	void archiveCriteriaAreImmutableIntersectingAndPreserveStashOrder() {
		final Parent first = new Parent("first", true);
		final Parent second = new Parent("second", true);
		final Stash stash = new Stash(List.of(
				new ArchiveGear<>(archive(FIRST_ID),
						List.of(new GearNode<>(first, source(FIRST_ID, "first"), List.of()))),
				new ArchiveGear<>(archive(SECOND_ID),
						List.of(new GearNode<>(second, source(SECOND_ID, "second"), List.of())))));
		final Query<Parent> all = stash.query(Parent.class);

		final Query<Parent> reversedSelection = all.where(List.of(SECOND_ID, FIRST_ID));
		final Query<Parent> intersection = reversedSelection.where(SECOND_ID);

		assertEquals(List.of(first, second), all.pull().gear());
		assertEquals(List.of(first, second), reversedSelection.pull().gear());
		assertEquals(List.of(second), intersection.pull().gear());
	}

	@Test
	void rejectsUnknownArchiveCriteria() {
		final Stash stash = stash(new GearNode<>(new Parent("first", true), source(FIRST_ID, "first"), List.of()));

		final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
				() -> stash.query(Object.class).where(ArchiveId.of("unknown")));

		assertEquals("Unknown archive: unknown", failure.getMessage());
	}

	private static Stash stash(final GearNode<Object> root) {
		return new Stash(List.of(new ArchiveGear<>(archive(FIRST_ID), List.of(root))));
	}

	private static ArchiveDescriptor archive(final ArchiveId id) {
		return ArchiveDescriptor.of(id, Path.of(id.value()));
	}

	private static ARI source(final ArchiveId archiveId, final String path) {
		return ARI.of("collection", archiveId, Path.of(path));
	}

	private interface BaseGear {

		boolean working();
	}

	private record Parent(String name, boolean working) implements BaseGear {
	}

	private record Child(String name, boolean working) implements BaseGear {
	}
}
