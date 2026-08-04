package com.retrocrawler.core.gear;

import java.util.Objects;
import java.util.Optional;

import com.retrocrawler.core.archive.clues.Confidence;

/**
 * A parser result for zero or one value of a fact type.
 *
 * @param <T>
 *            the type of the parsed value
 */
public final class RatedFact<T> {

	private final T value;

	private final Confidence confidence;

	private final String explanation;

	private RatedFact(final T value, final Confidence confidence, final String explanation) {
		this.value = value;
		this.confidence = Objects.requireNonNull(confidence, "confidence");
		this.explanation = explanation;
		if (confidence == Confidence.NONE && value != null) {
			throw new IllegalArgumentException("An unsuccessful rated fact must not contain a value.");
		}
		if (confidence != Confidence.NONE && value == null) {
			throw new IllegalArgumentException("A successful rated fact must contain a value.");
		}
	}

	public Optional<T> value() {
		return Optional.ofNullable(value);
	}

	public Confidence confidence() {
		return confidence;
	}

	public Optional<String> explanation() {
		return Optional.ofNullable(explanation);
	}

	public static <T> RatedFact<T> exact(final T value) {
		return successful(value, Confidence.EXACT);
	}

	public static <T> RatedFact<T> strong(final T value) {
		return successful(value, Confidence.STRONG);
	}

	public static <T> RatedFact<T> weak(final T value) {
		return successful(value, Confidence.WEAK);
	}

	public static <T> RatedFact<T> none(final String explanation) {
		return new RatedFact<>(null, Confidence.NONE, explanation);
	}

	private static <T> RatedFact<T> successful(final T value, final Confidence confidence) {
		return new RatedFact<>(Objects.requireNonNull(value, "value"), confidence, null);
	}

}
