package com.retrocrawler.core.gear.trace;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.retrocrawler.core.archive.clues.Artifact;
import com.retrocrawler.core.archive.clues.Clues;
import com.retrocrawler.core.gear.Confidence;
import com.retrocrawler.core.gear.Fact;

/**
 * Immutable explanation of how one artifact became one Gear occurrence.
 * <p>
 * Detection attributes are those available while Gear matchers run. Resolved
 * attributes are produced after selecting a Gear type and enabling its
 * contextual facts.
 */
public record ResolutionTrace(Artifact artifact, Attributes detection, List<Match> matches, Selection selection,
		Attributes resolved, List<Issue> issues) {

	public ResolutionTrace {
		Objects.requireNonNull(artifact, "artifact");
		Objects.requireNonNull(detection, "detection");
		matches = List.copyOf(Objects.requireNonNull(matches, "matches"));
		Objects.requireNonNull(selection, "selection");
		Objects.requireNonNull(resolved, "resolved");
		issues = List.copyOf(Objects.requireNonNull(issues, "issues"));
		final boolean selectedByMatcher = matches.stream().anyMatch(
				match -> selection.gearType().equals(match.gearType()) && match.confidence() != Confidence.NONE);
		if (selection.kind().isMatch() != selectedByMatcher) {
			throw new IllegalArgumentException(
					"The selection kind must agree with whether the selected Gear has a successful recorded match.");
		}
	}

	/**
	 * The selected Gear matcher, absent when the fallback Gear was selected.
	 */
	public Optional<Match> selectedMatch() {
		return matches.stream().filter(match -> selection.gearType().equals(match.gearType())).findFirst();
	}

	/**
	 * The interpreted facts and evidence left unresolved in one resolution
	 * pass.
	 */
	public record Attributes(List<Fact> facts, Clues unresolvedClues) {

		public Attributes {
			facts = List.copyOf(Objects.requireNonNull(facts, "facts"));
			Objects.requireNonNull(unresolvedClues, "unresolvedClues");
		}
	}

	/**
	 * One Gear matcher decision, including candidates rejected with
	 * {@code NONE}.
	 */
	public record Match(Class<?> gearType, Class<?> matcherType, Confidence confidence) {

		public Match {
			Objects.requireNonNull(gearType, "gearType");
			Objects.requireNonNull(matcherType, "matcherType");
			Objects.requireNonNull(confidence, "confidence");
		}
	}

	/** The Gear type ultimately selected and why it was selected. */
	public record Selection(Class<?> gearType, SelectionKind kind) {

		public Selection {
			Objects.requireNonNull(gearType, "gearType");
			Objects.requireNonNull(kind, "kind");
		}
	}

	/** A non-fatal ambiguity encountered while producing this Gear. */
	public record Issue(Phase phase, IssueKind kind, String explanation) {

		public Issue {
			Objects.requireNonNull(phase, "phase");
			Objects.requireNonNull(kind, "kind");
			if (Objects.requireNonNull(explanation, "explanation").isBlank()) {
				throw new IllegalArgumentException("A resolution issue explanation must not be blank.");
			}
		}
	}

	public enum Phase {
		DETECTION,
		MATCHING,
		RESOLUTION
	}

	public enum IssueKind {
		AMBIGUOUS_FACT,
		AMBIGUOUS_GEAR_MATCH
	}

	/** Why a Gear type was selected. */
	public enum SelectionKind {
		MATCH(true),
		MOST_SPECIFIC_MATCH(true),
		FALLBACK_NO_MATCH(false),
		FALLBACK_AMBIGUOUS_MATCH(false);

		private final boolean match;

		SelectionKind(final boolean match) {
			this.match = match;
		}

		private boolean isMatch() {
			return match;
		}
	}
}
