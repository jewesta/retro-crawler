package com.retrocrawler.core.progress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class ProgressorTest {

	@Test
	void publishesStructuredSnapshotsAndMessageOnlyObservations() {
		final List<String> messages = new ArrayList<>();
		final List<ProgressSnapshot> snapshots = new ArrayList<>();
		final Progressor progressor = Progressor.reportingMessages(messages::add)
				.withMonitor(progress -> snapshots.add(progress.snapshot()));

		progressor.begin(ProgressStage.CRAWLING, "Starting.", 10, ProgressAccuracy.APPROXIMATE);
		progressor.advanceTo(1, "Region 2 of 10.");

		final ProgressSnapshot progress = snapshots.getLast();
		assertEquals(List.of("Starting.", "Region 2 of 10."), messages);
		assertEquals(ProgressStage.CRAWLING, progress.stage());
		assertEquals(ProgressAccuracy.APPROXIMATE, progress.accuracy());
		assertEquals(1, progress.completed());
		assertEquals(10, progress.total());
		assertEquals(0.1, progress.stageFraction().orElseThrow());
		assertEquals(0.1, progress.overallFraction());
		assertTrue(progress.remaining().isPresent());
	}

	@Test
	void retainsThePepperControllerScaleAndBaseConversions() {
		final Progressor progressor = Progressor.create();
		final ProgressController controller = progressor;

		controller.reset(8).advanceBy(2);
		assertEquals(0.25, progressor.progress());

		controller.advanceToBase100(50);
		assertEquals(0.5, progressor.progress());

		controller.advanceToBase1(0.75);
		assertEquals(0.75, progressor.progress());

		controller.advanceToBase(12, 0);
		assertEquals(0, progressor.progress());
	}

	@Test
	void aSubProgressorUsesItsOwnScaleInsideTheRootWindow() {
		final Progressor root = Progressor.create();
		final Progressor secondHalf = root.splitIntoEqualParts(2)[1];

		secondHalf.reset(4).advanceBy(1);

		assertEquals(0.25, secondHalf.progress());
		assertEquals(0.625, root.progress());
		assertEquals(0.25, secondHalf.snapshot().progress());
		assertEquals(0.625, secondHalf.snapshot().overallFraction());
		assertEquals(1, root.snapshot().completed());
		assertEquals(4, root.snapshot().total());
	}

	@Test
	void resetCanMoveProgressBackToTheStart() {
		final Progressor progressor = Progressor.create();
		progressor.reset(10).advanceToEnd();

		progressor.reset(20);

		assertEquals(0, progressor.progress());
		assertEquals(0, progressor.completed());
		assertEquals(20, progressor.total());
	}

	@Test
	void serializesParallelAdvancesLikeThePepperImplementation() {
		final Progressor progressor = Progressor.create().reset(1_000);

		IntStream.range(0, 1_000).parallel().forEach(ignored -> progressor.advance());

		assertEquals(1, progressor.progress());
		assertEquals(1_000, progressor.completed());
	}

	@Test
	void dummyProgressorDoesNothingAndReturnsDummyChildren() {
		Progressor.DUMMY.begin(ProgressStage.CRAWLING, "Ignored.", 10, ProgressAccuracy.EXACT).advanceToEnd();

		assertEquals(0, Progressor.DUMMY.progress());
		assertTrue(java.util.Arrays.stream(Progressor.DUMMY.splitIntoEqualParts(3))
				.allMatch(child -> child == Progressor.DUMMY));
	}

	@Test
	void cancellationIsOneWayAndStopsFurtherProgress() {
		final List<ProgressSnapshot> snapshots = new ArrayList<>();
		final Progressor progressor = Progressor.observing(progress -> snapshots.add(progress.snapshot()));

		progressor.indeterminate(ProgressStage.PLANNING, "Planning.");
		progressor.cancel("Stopping.");
		progressor.cancel("Ignored.");

		assertTrue(progressor.isCancelled());
		assertEquals(2, snapshots.size());
		assertEquals(ProgressState.CANCELLED, snapshots.getLast().state());
		assertEquals("Stopping.", snapshots.getLast().message());
		assertThrows(ProgressCancelledException.class, progressor::throwIfCancelled);
		assertThrows(ProgressCancelledException.class,
				() -> progressor.indeterminate(ProgressStage.CRAWLING, "Ignored."));
	}

	@Test
	void representsIndeterminateProgressWithoutInventingAStageFractionOrEta() {
		final Progressor progressor = Progressor.create();

		progressor.indeterminate(ProgressStage.PLANNING, "Planning.");

		final ProgressSnapshot progress = progressor.snapshot();
		assertFalse(progress.isDeterminate());
		assertTrue(progress.stageFraction().isEmpty());
		assertTrue(progress.remaining().isEmpty());
	}

	@Test
	void mapsNestedWeightedProgressIntoTheRootWindow() {
		final Progressor root = Progressor.create();
		final Progressor[] halves = root.splitIntoEqualParts(2);
		final Progressor[] secondHalf = halves[1].splitInRelationTo(1, 3);

		halves[0].begin(ProgressStage.of("FIRST"), "First.", 10, ProgressAccuracy.EXACT)
				.advanceTo(5, "Half of the first half.").complete("First done.");
		assertEquals(0.5, root.snapshot().overallFraction());
		assertEquals(ProgressState.RUNNING, root.snapshot().state());
		assertEquals(ProgressStage.of("FIRST"), root.snapshot().stage());

		secondHalf[0].begin(ProgressStage.of("SECOND_A"), "Second A.", 4, ProgressAccuracy.EXACT)
				.advanceToEnd("Second A done.");
		assertEquals(0.625, root.snapshot().overallFraction());

		secondHalf[1].begin(ProgressStage.of("SECOND_B"), "Second B.", 8, ProgressAccuracy.EXACT)
				.advanceToEnd("Second B done.");
		assertEquals(1, root.snapshot().overallFraction());
	}

	@Test
	void rootCancellationRetainsTheActiveChildStage() {
		final Progressor root = Progressor.create();
		final Progressor child = root.splitIntoEqualParts(2)[0];
		child.begin(ProgressStage.of("CHILD"), "Working.", 4, ProgressAccuracy.EXACT).advanceTo(1, "One done.");

		root.cancel("Stopping.");

		assertEquals(ProgressStage.of("CHILD"), root.snapshot().stage());
		assertEquals(1, root.snapshot().completed());
		assertEquals(ProgressState.CANCELLED, root.snapshot().state());
		assertTrue(root.snapshot().remaining().isEmpty());
	}

	@Test
	void fixedStepMonitorThrottlesWithinAStageButReportsStageAndStateChanges() {
		final List<String> observations = new ArrayList<>();
		final FixedStepProgressMonitor monitor = new FixedStepProgressMonitor(4) {

			@Override
			protected void monitorFixedStep(final long maximumStep, final long currentStep,
					final ProgressSnapshot progress) {
				observations.add(progress.stage() + ":" + currentStep + ":" + progress.state());
			}
		};
		final Progressor progressor = Progressor.observing(monitor);

		progressor.begin(ProgressStage.CRAWLING, "Starting.", 100, ProgressAccuracy.APPROXIMATE);
		progressor.advanceTo(10, "Same quarter.");
		progressor.advanceTo(25, "Second quarter.");
		progressor.indeterminate(ProgressStage.STOWING, "Stowing.");
		progressor.complete("Done.");

		assertEquals(List.of("CRAWLING:0:RUNNING", "CRAWLING:1:RUNNING", "STOWING:-1:RUNNING", "STOWING:-1:COMPLETE"),
				observations);
	}

	@Test
	void reportsCompleteAndFailedTerminalStates() {
		final Progressor complete = Progressor.create();
		complete.begin(ProgressStage.RESOLVING, "Resolving.", 2, ProgressAccuracy.EXACT).advanceTo(1, "One resolved.");
		complete.complete("Done.");

		assertEquals(ProgressState.COMPLETE, complete.snapshot().state());
		assertEquals(2, complete.snapshot().completed());
		assertEquals(1, complete.snapshot().overallFraction());

		final Progressor failed = Progressor.create();
		failed.indeterminate(ProgressStage.STOWING, "Stowing.");
		failed.fail("Repository unavailable.");

		assertEquals(ProgressState.FAILED, failed.snapshot().state());
		assertEquals("Repository unavailable.", failed.snapshot().message());
		assertTrue(failed.snapshot().hasFinished());
	}
}
