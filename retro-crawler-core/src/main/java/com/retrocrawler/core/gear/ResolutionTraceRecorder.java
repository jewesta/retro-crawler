package com.retrocrawler.core.gear;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.retrocrawler.core.archive.clues.Artifact;
import com.retrocrawler.core.gear.injector.GearSpecialist;
import com.retrocrawler.core.gear.trace.ResolutionTrace;
import com.retrocrawler.core.gear.trace.ResolutionTrace.Attributes;
import com.retrocrawler.core.gear.trace.ResolutionTrace.Issue;
import com.retrocrawler.core.gear.trace.ResolutionTrace.IssueKind;
import com.retrocrawler.core.gear.trace.ResolutionTrace.Match;
import com.retrocrawler.core.gear.trace.ResolutionTrace.Phase;

/** Collects one trace while {@link GearResolver} resolves one artifact. */
final class ResolutionTraceRecorder {

	private final Artifact artifact;

	private Attributes detection;

	private final List<Match> matches = new ArrayList<>();

	private Match selectedMatch;

	private Attributes resolved;

	private final List<Issue> issues = new ArrayList<>();

	ResolutionTraceRecorder(final Artifact artifact) {
		this.artifact = Objects.requireNonNull(artifact, "artifact");
	}

	void attributes(final Phase phase, final RetroAttributes attributes) {
		Objects.requireNonNull(phase, "phase");
		Objects.requireNonNull(attributes, "attributes");
		final Attributes snapshot = new Attributes(
				attributes.facts().stream().sorted(Comparator.comparing(Fact::key)).toList(), attributes.clues());
		switch (phase) {
		case DETECTION -> detection = snapshot;
		case RESOLUTION -> resolved = snapshot;
		case MATCHING -> throw new IllegalArgumentException("Matching does not produce an attribute snapshot.");
		}
	}

	void match(final GearSpecialist specialist, final Confidence confidence) {
		Objects.requireNonNull(specialist, "specialist");
		final GearDescriptor descriptor = specialist.gearDefinition();
		matches.add(new Match(descriptor.type(), descriptor.matcher().getClass(), confidence));
	}

	void selected(final Class<?> gearType) {
		Objects.requireNonNull(gearType, "gearType");
		selectedMatch = matches.stream().filter(match -> gearType.equals(match.gearType())).findFirst().orElseThrow(
				() -> new IllegalArgumentException("Selected Gear type has no recorded match: " + gearType));
		final List<Class<?>> tied = matches.stream().filter(
				match -> match.confidence() == selectedMatch.confidence() && match.confidence() != Confidence.NONE)
				.map(Match::gearType).toList();
		if (tied.size() > 1) {
			issues.add(new Issue(Phase.MATCHING, IssueKind.AMBIGUOUS_GEAR_MATCH, "Equal best confidence "
					+ selectedMatch.confidence() + " for Gear types: " + tied.stream().map(Class::getName).toList()));
		}
	}

	void ambiguousFact(final Phase phase, final String explanation) {
		issues.add(new Issue(phase, IssueKind.AMBIGUOUS_FACT, explanation));
	}

	ResolutionTrace trace() {
		return new ResolutionTrace(artifact, Objects.requireNonNull(detection, "detection"), matches,
				Objects.requireNonNull(selectedMatch, "selectedMatch"), Objects.requireNonNull(resolved, "resolved"),
				issues);
	}
}
