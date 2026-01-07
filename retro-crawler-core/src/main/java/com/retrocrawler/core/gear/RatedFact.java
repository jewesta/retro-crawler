package com.retrocrawler.core.gear;

import java.util.Objects;
import java.util.Optional;

import com.retrocrawler.core.archive.clues.Confidence;

public class RatedFact {

	private final Object value;

	private final Confidence confidence;

	private final String explanation;

	private RatedFact(final Object fact, final Confidence confidence, final String explanation) {
		this.value = fact;
		this.confidence = Objects.requireNonNull(confidence);
		this.explanation = explanation;
	}

	public Optional<Object> getValue() {
		return Optional.ofNullable(value);
	}

	public Confidence getConfidence() {
		return confidence;
	}

	public Optional<String> getExplanation() {
		return Optional.ofNullable(explanation);
	}

	public static RatedFact exact(final Object value) {
		Objects.requireNonNull(value);
		return new RatedFact(value, Confidence.EXACT, null);
	}

	public static RatedFact strong(final Object value) {
		Objects.requireNonNull(value);
		return new RatedFact(value, Confidence.STRONG, null);
	}

	public static RatedFact weak(final Object value) {
		Objects.requireNonNull(value);
		return new RatedFact(value, Confidence.WEAK, null);
	}

	public static RatedFact none(final String explanation) {
		return new RatedFact(null, Confidence.NONE, explanation);
	}

}
