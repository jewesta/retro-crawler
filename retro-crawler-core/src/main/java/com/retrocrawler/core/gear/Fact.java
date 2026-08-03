package com.retrocrawler.core.gear;

import java.util.Objects;
import java.util.Set;

import com.retrocrawler.core.archive.clues.Clue;
import com.retrocrawler.core.archive.clues.Confidence;
import com.retrocrawler.core.util.RetroAttribute;

/**
 * A model-dependent, typed interpretation of a {@link Clue}.
 * <p>
 * Facts are produced during gear resolution and are not stored in the clue
 * archive. Every fact retains the evidence from which it was derived so that
 * resolved gear remains traceable to the archive.
 */
public final class Fact implements RetroAttribute {

	private final String key;
	private final Set<Object> value;
	private final Confidence confidence;
	private final Clue source;

	public Fact(final String key, final Set<Object> value, final Confidence confidence, final Clue source) {
		this.key = Objects.requireNonNull(key, "key");
		Objects.requireNonNull(value, "value");
		if (value.isEmpty()) {
			throw new IllegalArgumentException("A fact must contain at least one interpreted value.");
		}
		this.value = Set.copyOf(value);
		this.confidence = Objects.requireNonNull(confidence, "confidence");
		this.source = Objects.requireNonNull(source, "source");
	}

	@Override
	public String key() {
		return key;
	}

	@Override
	public Set<Object> value() {
		return value;
	}

	public Confidence confidence() {
		return confidence;
	}

	public Clue source() {
		return source;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[key=" + key + ", value=" + value + "]";
	}

}
